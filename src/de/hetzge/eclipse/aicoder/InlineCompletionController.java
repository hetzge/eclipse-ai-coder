package de.hetzge.eclipse.aicoder;

import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IPaintPositionManager;
import org.eclipse.jface.text.IPainter;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension2;
import org.eclipse.jface.text.ITextViewerExtension5;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CaretEvent;
import org.eclipse.swt.custom.CaretListener;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.StyledTextLineSpacingProvider;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GlyphMetrics;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.contexts.IContextActivation;
import org.eclipse.ui.texteditor.ITextEditor;

import de.hetzge.eclipse.aicoder.Context.ContextEntry;
import de.hetzge.eclipse.aicoder.Context.RootContextEntry;

public final class InlineCompletionController {

	private static final Map<ITextViewer, InlineCompletionController> CONTROLLER_BY_VIEWER;
	static {
		CONTROLLER_BY_VIEWER = new ConcurrentHashMap<>();
	}

	public static InlineCompletionController setup(ITextEditor textEditor) {
		final ITextViewer textViewer = EclipseUtils.getTextViewer(textEditor);
		return CONTROLLER_BY_VIEWER.computeIfAbsent(textViewer, ignore -> {

			final Font font = Display.getDefault().syncCall(() -> {
				final FontData[] fontData = textViewer.getTextWidget().getFont().getFontData();
				for (int i = 0; i < fontData.length; ++i) {
					fontData[i].setStyle(fontData[i].getStyle() | SWT.ITALIC);
				}
				return new Font(textViewer.getTextWidget().getDisplay(), fontData);
			});

			final InlineCompletionController controller = new InlineCompletionController(textViewer, textEditor, font);
			Display.getDefault().syncExec(() -> {
				((ITextViewerExtension2) textViewer).addPainter(controller.painter);
				textViewer.getTextWidget().addPaintListener(controller.paintListener);
				textViewer.getTextWidget().setLineSpacingProvider(controller.spacingProvider);
				textViewer.getTextWidget().addKeyListener(controller.keyListener);
				textViewer.getSelectionProvider().addSelectionChangedListener(controller.selectionListener);
				textViewer.getTextWidget().addCaretListener(controller.caretListener);
				textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput()).addDocumentListener(controller.documentListener);
			});
			return controller;
		});
	}

	private final ITextViewer textViewer;
	private final ITextEditor textEditor;
	private final StyledText widget;
	private final StyledTextLineSpacingProviderImplementation spacingProvider;
	private final DocumentListenerImplementation documentListener;
	private final PaintListenerImplementation paintListener;
	private final PainterImplementation painter;
	private final KeyListenerImplementation keyListener;
	private final ISelectionChangedListener selectionListener;
	private final CaretListener caretListener;
	private final Font font;
	private Completion completion;
	private IContextActivation context;
	private Job job;
	private Runnable debounceRunnable;

	private InlineCompletionController(ITextViewer textViewer, ITextEditor textEditor, Font font) {
		this.textViewer = textViewer;
		this.textEditor = textEditor;
		this.widget = textViewer.getTextWidget();
		this.spacingProvider = new StyledTextLineSpacingProviderImplementation();
		this.documentListener = new DocumentListenerImplementation();
		this.paintListener = new PaintListenerImplementation();
		this.painter = new PainterImplementation();
		this.keyListener = new KeyListenerImplementation();
		this.selectionListener = new SelectionListenerImplementation();
		this.caretListener = new CaretListenerImplementation();
		this.font = font;
		this.completion = null;
		this.context = null;
		this.job = null;
	}

	public void trigger() {

		// Filter JDK
		// Prefer local
		// Unresolved types?!
		// Comment complete (when newline after comment, calculate completion, optional
		// remove comment, always use multiline)
		// Comment complete "?!" trigger
		// Cache completion

		final long startTime = System.currentTimeMillis();
		final int modelOffset = EclipseUtils.getCurrentOffsetInDocument(InlineCompletionController.this.textEditor);
		final IDocument document = this.textViewer.getDocument();

		abort();
		this.job = Job.create("AI inline completion", monitor -> {
			final RootContextEntry rootContextEntry;
			try {
				rootContextEntry = RootContextEntry.create(document, this.textEditor.getEditorInput(), modelOffset);
			} catch (final CoreException | UnsupportedFlavorException | IOException | BadLocationException exception) {
				AiCoderActivator.log().error("AI Coder completion failed", exception);
				final String stacktrace = Utils.getStacktraceString(exception);
				addHistoryEntry("", stacktrace, "Error: " + Optional.ofNullable(exception.getMessage()).orElse("-"), 0, 0);
				return;
			}

			int widgetLine;
			try {
				widgetLine = getWidgetLine(modelOffset);
			} catch (final BadLocationException exception) {
				AiCoderActivator.log().error("AI Coder completion failed", exception);
				final String stacktrace = Utils.getStacktraceString(exception);
				addHistoryEntry("", stacktrace, "Error: " + Optional.ofNullable(exception.getMessage()).orElse("-"), 0, 0);
				return;
			}
			final int widgetOffset = getWidgetOffset(modelOffset);

			final int lineHeight = Display.getDefault().syncCall(() -> InlineCompletionController.this.textViewer.getTextWidget().getLineHeight());
			final int defaultLineSpacing = Display.getDefault().syncCall(() -> InlineCompletionController.this.textViewer.getTextWidget().getLineSpacing());

			if (monitor.isCanceled()) {
				return;
			}
			final String contextString = ContextEntry.apply(rootContextEntry);

			// IMPORTANT: DO this after ContextEntry.apply(...)
			updateContextView(rootContextEntry);

			final String[] contextParts = contextString.split(Context.SuffixContextEntry.FILL_HERE_PLACEHOLDER);
			final String input = contextString; // Use full context as input

			try {
				final String prefix = contextParts[0];
				final String suffix = contextParts.length > 1 ? contextParts[1] : "";
				final long llmStartTime = System.currentTimeMillis();
				final String content = LlmUtils.execute(prefix, suffix);
				final long duration = System.currentTimeMillis() - startTime;
				final long llmDuration = System.currentTimeMillis() - llmStartTime;
				final int currentModelOffset = EclipseUtils.getCurrentOffsetInDocument(InlineCompletionController.this.textEditor);
				final boolean isBlank = content.isBlank();
				final boolean isMoved = currentModelOffset != modelOffset;
				if (!isBlank && !isMoved) {
					setup(Completion.create(document, modelOffset, widgetOffset, widgetLine, content, lineHeight, defaultLineSpacing));
				}
				String status;
				if (isMoved) {
					status = "Moved";
				} else if (isBlank) {
					status = "Blank";
				} else {
					status = "Generated";
				}
				addHistoryEntry(input, content, status, duration, llmDuration);
			} catch (final IOException | BadLocationException exception) {
				AiCoderActivator.log().error("AI Coder completion failed", exception);
				final long duration = System.currentTimeMillis() - startTime;
				final String stacktrace = Utils.getStacktraceString(exception);
				addHistoryEntry(input, stacktrace, "Error: " + Optional.ofNullable(exception.getMessage()).orElse("-"), duration, 0);
			}
		});
		this.job.schedule();
	}

	private void updateContextView(final RootContextEntry rootContextEntry) {
		Display.getDefault().syncExec(() -> {
			try {
				ContextView.get().ifPresent(view -> {
					view.setRootContextEntry(rootContextEntry);
				});
			} catch (final CoreException exception) {
				throw new RuntimeException(exception);
			}
		});
	}

	private int getWidgetLine(int modelOffset) throws BadLocationException {
		if (this.textViewer instanceof final ITextViewerExtension5 extension5) {
			return extension5.modelLine2WidgetLine(this.textViewer.getDocument().getLineOfOffset(modelOffset));
		} else {
			return this.textViewer.getDocument().getLineOfOffset(modelOffset);
		}
	}

	private int getWidgetOffset(int modelOffset) {
		if (this.textViewer instanceof final ITextViewerExtension5 extension5) {
			return extension5.modelOffset2WidgetOffset(modelOffset);
		} else {
			return modelOffset;
		}
	}

	private void addHistoryEntry(String input, String output, String status, long duration, long llmDuration) {
		Display.getDefault().asyncExec(() -> {
			final IEditorInput editorInput = this.textEditor.getEditorInput();
			final String filePath = editorInput.getName();

			// Calculate input stats
			final String[] inputWords = input.split("\\s+");
			final int inputWordCount = inputWords.length;
			final long inputLineCount = input.lines().count();

			// Calculate output stats
			final String[] outputWords = output.split("\\s+");
			final int outputWordCount = outputWords.length;
			final long outputLineCount = output.lines().count();

			final AiCoderHistoryEntry entry = new AiCoderHistoryEntry(
					LocalDateTime.now(),
					AiCoderPreferences.getAiProvider(),
					filePath,
					status,
					input,
					input.length(),
					inputWordCount,
					(int) inputLineCount,
					output,
					output.length(),
					outputWordCount,
					(int) outputLineCount,
					duration,
					llmDuration);

			AiCoderHistoryView.get().ifPresent(view -> {
				view.addHistoryEntry(entry);
			});
		});
	}

	private void setup(Completion completion) {
		AiCoderActivator.log().info("Activate context");
		this.completion = completion;
		Display.getDefault().syncExec(() -> {
			this.context = EclipseUtils.getContextService(this.textEditor).activateContext("de.hetzge.eclipse.codestral.inlineCompletionVisible");
		});
	}

	public boolean abort() {
		this.paintListener.resetMetrics();
		if (this.job != null) {
			AiCoderActivator.log().info("Abort job");
			this.job.cancel();
			this.job = null;
		}
		if (this.debounceRunnable != null) {
			AiCoderActivator.log().info("Abort debounce");
			Display.getCurrent().timerExec(-1, this.debounceRunnable);
		}
		if (this.context != null) {
			AiCoderActivator.log().info("Deactivate context");
			EclipseUtils.getContextService(this.textEditor).deactivateContext(this.context);
			this.context = null;
		}
		if (this.completion != null) {
			AiCoderActivator.log().info("Unset completion");
			this.completion = null;
			return true;
		}
		return false;
	}

	public void accept() throws CoreException {
		if (this.completion == null) {
			return;
		}
		final IDocument document = this.textViewer.getDocument();
		try {
			final Completion completion = this.completion;
			document.replace(completion.modelRegion().getOffset(), completion.modelRegion().getLength(), this.completion.content()); // triggers document change -> triggers abort
			this.textViewer.setSelectedRange(completion.modelRegion().getOffset() + completion.content().length(), 0);

			AiCoderHistoryView.get().ifPresent(view -> {
				view.setLatestAccepted();
			});
		} catch (final BadLocationException exception) {
			throw new CoreException(Status.error("Failed to accept inline completion", exception));
		}
	}

	private class DocumentListenerImplementation implements IDocumentListener {
		@Override
		public void documentAboutToBeChanged(DocumentEvent event) {
		}

		@Override
		public void documentChanged(DocumentEvent event) {
			abort();
		}
	}

	private class KeyListenerImplementation implements KeyListener {

		private final Set<Integer> activeKeyCodes;

		public KeyListenerImplementation() {
			this.activeKeyCodes = new HashSet<>();
		}

		@Override
		public void keyPressed(KeyEvent event) {
			this.activeKeyCodes.add(event.keyCode);
		}

		@Override
		public void keyReleased(KeyEvent event) {
			final boolean isCommandDown = this.activeKeyCodes.stream().anyMatch(this::isCommandKey);
			this.activeKeyCodes.remove(event.keyCode);
			abort();
			// Ignore command keys
			if (isCommandDown) {
				return;
			}
			// Handle "tab" only if no completion context is active
			if (InlineCompletionController.this.context != null && event.keyCode == 9) {
				return;
			}
			if (AiCoderPreferences.isAutocompleteEnabled()) {
				InlineCompletionController.this.debounceRunnable = () -> {
					if (isNoSelectionActive() && isLineSuffixBlank()) {
						trigger();
					}
				};
				Display.getCurrent().timerExec((int) AiCoderPreferences.getDebounceDuration().toMillis(), InlineCompletionController.this.debounceRunnable);
			}
		}

		private boolean isNoSelectionActive() {
			return InlineCompletionController.this.textViewer.getSelectedRange().y == 0;
		}

		private boolean isLineSuffixBlank() {
			try {
				final int modelOffset = EclipseUtils.getCurrentOffsetInDocument(InlineCompletionController.this.textEditor);
				final IRegion lineRegion = InlineCompletionController.this.textViewer.getDocument().getLineInformationOfOffset(modelOffset);
				final String lineString = InlineCompletionController.this.textViewer.getDocument().get(lineRegion.getOffset(), lineRegion.getLength());
				return lineString.substring(modelOffset - lineRegion.getOffset()).replace(";", "").replace(")", "").replace("{", "").replace("{", "").isBlank();
			} catch (final BadLocationException exception) {
				throw new RuntimeException("Failed to check if line suffix is blank", exception);
			}
		}

		private boolean isCommandKey(int keyCode) {
			return keyCode == SWT.CTRL
					|| keyCode == SWT.ALT
					|| keyCode == SWT.SHIFT
					|| keyCode == SWT.COMMAND
					|| keyCode == SWT.ESC;
		}
	}

	private class StyledTextLineSpacingProviderImplementation implements StyledTextLineSpacingProvider {
		@Override
		public Integer getLineSpacing(int lineIndex) {
			final Completion completion = InlineCompletionController.this.completion;
			if (completion != null && completion.lineIndex() == lineIndex) {
				return completion.lineSpacing();
			}
			return null;
		}
	}

	private class PaintListenerImplementation implements PaintListener {
		private final Set<GlyphMetrics> modifiedMetrics;

		public PaintListenerImplementation() {
			this.modifiedMetrics = new HashSet<>();
		}

		@Override
		public void paintControl(PaintEvent event) {
			final Completion completion = InlineCompletionController.this.completion;
			final StyledText widget = InlineCompletionController.this.textViewer.getTextWidget();
			if (completion == null) {
				return;
			}
			final List<String> lines = completion.content().lines().toList();
			event.gc.setForeground(widget.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
			event.gc.setFont(InlineCompletionController.this.font);
			final Point location = widget.getLocationAtOffset(completion.widgetOffset());
			for (int i = 0; i < lines.size(); i++) {
				final String line = lines.get(i);
				if (i == 0) {
					// first line
					event.gc.drawText(completion.firstLineContent(), location.x, location.y, true);
					if (completion.firstLineSuffixCharacter() != null) {
						final int suffixCharacterWidth = event.gc.textExtent(completion.firstLineSuffixCharacter()).x;
						final int contentWidth = event.gc.textExtent(completion.firstLineContent()).x;
						final StyleRange styleRange = widget.getStyleRangeAtOffset(completion.widgetOffset());
						final int metricWidth = contentWidth + suffixCharacterWidth;
						if (needMetricUpdate(styleRange, metricWidth)) {
							updateMetrics(event, completion, widget, metricWidth);
						}
						event.gc.drawText(completion.firstLineSuffixCharacter(), location.x + contentWidth, location.y, false);
					}
				} else {
					event.gc.drawText(line.replace("\t", " ".repeat(InlineCompletionController.this.widget.getTabs())), 0, location.y + i * completion.lineHeight(), true);
				}
			}
		}

		private boolean needMetricUpdate(final StyleRange styleRange, final int metricWidth) {
			return styleRange == null || styleRange.metrics == null || styleRange.metrics.width != metricWidth;
		}

		private void updateMetrics(PaintEvent event, Completion completion, StyledText widget, int metricWidth) {
			final FontMetrics fontMetrics = event.gc.getFontMetrics();
			final StyleRange newStyleRange = new StyleRange(completion.widgetOffset(), 1, null, null);
			newStyleRange.metrics = new GlyphMetrics(fontMetrics.getAscent(), fontMetrics.getDescent(), metricWidth);
			widget.setStyleRange(newStyleRange);
			this.modifiedMetrics.add(newStyleRange.metrics);
		}

		public void resetMetrics() {
			final StyledText widget = InlineCompletionController.this.textViewer.getTextWidget();
			final StyleRange[] styleRanges = widget.getStyleRanges();
			for (final StyleRange styleRange : styleRanges) {
				if (this.modifiedMetrics.contains(styleRange.metrics)) {
					styleRange.metrics = null;
					widget.setStyleRange(styleRange);
				}
			}
			this.modifiedMetrics.clear();
		}
	}

	private class PainterImplementation implements IPainter {
		@Override
		public void dispose() {
			InlineCompletionController.this.font.dispose();
			CONTROLLER_BY_VIEWER.remove(InlineCompletionController.this.textViewer);
		}

		@Override
		public void paint(int reason) {
		}

		@Override
		public void deactivate(boolean redraw) {
		}

		@Override
		public void setPositionManager(IPaintPositionManager manager) {
		}
	}

	private class SelectionListenerImplementation implements ISelectionChangedListener {
		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			abort();
		}
	}

	private class CaretListenerImplementation implements CaretListener {
		@Override
		public void caretMoved(CaretEvent event) {
			abort();
		}
	}
}
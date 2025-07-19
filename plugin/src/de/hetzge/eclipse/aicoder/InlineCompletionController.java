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
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension2;
import org.eclipse.jface.text.ITextViewerExtension5;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CaretEvent;
import org.eclipse.swt.custom.CaretListener;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.StyledTextLineSpacingProvider;
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

import de.hetzge.eclipse.aicoder.context.ContextContext;
import de.hetzge.eclipse.aicoder.context.ContextEntry;
import de.hetzge.eclipse.aicoder.context.RootContextEntry;
import de.hetzge.eclipse.aicoder.context.SuffixContextEntry;

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
	private final ISelectionChangedListener selectionListener;
	private final CaretListener caretListener;
	private final Font font;
	private Completion completion;
	private IContextActivation context;
	private Job job;
	private final Runnable debounceRunnable;
	private long changeCounter;
	private long lastChangeCounter;
	private final Debouncer debouncer;

	private InlineCompletionController(ITextViewer textViewer, ITextEditor textEditor, Font font) {
		this.textViewer = textViewer;
		this.textEditor = textEditor;
		this.widget = textViewer.getTextWidget();
		this.spacingProvider = new StyledTextLineSpacingProviderImplementation();
		this.documentListener = new DocumentListenerImplementation();
		this.paintListener = new PaintListenerImplementation();
		this.painter = new PainterImplementation();
		this.selectionListener = new SelectionListenerImplementation();
		this.caretListener = new CaretListenerImplementation();
		this.font = font;
		this.completion = null;
		this.context = null;
		this.job = null;
		this.debounceRunnable = null;
		this.changeCounter = 0;
		this.lastChangeCounter = 0;
		this.debouncer = new Debouncer(Display.getCurrent(), AiCoderPreferences::getDebounceDuration);
	}

	private void triggerAutocomplete() {
		final boolean isDocumentChanged = this.lastChangeCounter != this.changeCounter;
		this.lastChangeCounter = this.changeCounter;
		if (!AiCoderPreferences.isAutocompleteEnabled()) {
			return;
		}
		if (AiCoderPreferences.isOnlyOnChangeAutocompleteEnabled() && !isDocumentChanged) {
			return;
		}
		this.debouncer.debounce(() -> {
			if (isNoSelectionActive() && isAutocompleteAllowed()) {
				trigger();
			}
		});
	}

	public void trigger() {
		AiCoderActivator.log().info("Trigger");
		final long startTime = System.currentTimeMillis();
		abort("Trigger");
		this.job = Job.create("AI inline completion", monitor -> {
			String contextString = "";
			try {
				final int modelOffset = EclipseUtils.getCurrentOffsetInDocument(InlineCompletionController.this.textEditor);
				final IDocument document = this.textViewer.getDocument();
				final RootContextEntry rootContextEntry = RootContextEntry.create(document, this.textEditor.getEditorInput(), modelOffset);
				if (monitor.isCanceled()) {
					addHistoryEntry("", "", "Aborted", 0, 0);
					return;
				}
				final int widgetLine = getWidgetLine(modelOffset);
				final int widgetOffset = getWidgetOffset(modelOffset);
				final int lineHeight = Display.getDefault().syncCall(() -> InlineCompletionController.this.textViewer.getTextWidget().getLineHeight());
				final int defaultLineSpacing = Display.getDefault().syncCall(() -> InlineCompletionController.this.textViewer.getTextWidget().getLineSpacing());
				contextString = ContextEntry.apply(rootContextEntry, new ContextContext());
				// IMPORTANT: DO this after ContextEntry.apply(...)
				updateContextView(rootContextEntry);
				if (monitor.isCanceled()) {
					addHistoryEntry("", "", "Aborted", 0, 0);
					return;
				}
				final String[] contextParts = contextString.split(SuffixContextEntry.FILL_HERE_PLACEHOLDER);
				final String prefix = contextParts[0];
				final String suffix = contextParts.length > 1 ? contextParts[1] : "";
				final long llmStartTime = System.currentTimeMillis();
				final String content = LlmUtils.execute(prefix, suffix);
				final long duration = System.currentTimeMillis() - startTime;
				final long llmDuration = System.currentTimeMillis() - llmStartTime;
				final int currentModelOffset = EclipseUtils.getCurrentOffsetInDocument(InlineCompletionController.this.textEditor);
				final boolean isMultilineContent = content.contains("\n");
				final boolean isBlank = content.isBlank();
				final boolean isMoved = currentModelOffset != modelOffset;
				final boolean isSame = isMultilineContent && suffix.replaceAll("\\s", "").startsWith(content.replaceAll("\\s", ""));
				if (monitor.isCanceled()) {
					addHistoryEntry("", "", "Aborted", 0, 0);
					return;
				}
				if (!isBlank && !isMoved && !isSame) {
					setup(Completion.create(document, modelOffset, widgetOffset, widgetLine, content, lineHeight, defaultLineSpacing));
				}
				String status;
				if (isMoved) {
					status = "Moved";
				} else if (isBlank) {
					status = "Blank";
				} else if (isSame) {
					status = "Same";
				} else {
					status = "Generated";
				}
				addHistoryEntry(contextString, content, status, duration, llmDuration);
			} catch (final IOException | BadLocationException | UnsupportedFlavorException exception) {
				AiCoderActivator.log().error("AI Coder completion failed", exception);
				final long duration = System.currentTimeMillis() - startTime;
				final String stacktrace = Utils.getStacktraceString(exception);
				addHistoryEntry(contextString, stacktrace, "Error: " + Optional.ofNullable(exception.getMessage()).orElse("-"), duration, 0);
			}
		});
		this.job.schedule();
	}

	private boolean isNoSelectionActive() {
		return InlineCompletionController.this.textViewer.getSelectedRange().y == 0;
	}

	private boolean isAutocompleteAllowed() {
		try {
			final int modelOffset = EclipseUtils.getCurrentOffsetInDocument(InlineCompletionController.this.textEditor);
			final IDocument document = InlineCompletionController.this.textViewer.getDocument();
			if (modelOffset > 0 && modelOffset < document.getLength()) {
				if (document.getChar(modelOffset - 1) == '"' && document.getChar(modelOffset) == '"') {
					return true;
				}
				if (document.getChar(modelOffset - 1) == '(' && document.getChar(modelOffset) == ')') {
					return true;
				}
				if (document.getChar(modelOffset - 1) == '[' && document.getChar(modelOffset) == ']') {
					return true;
				}
				if (document.getChar(modelOffset - 1) == '{' && document.getChar(modelOffset) == '}') {
					return true;
				}
			}
			final IRegion lineRegion = document.getLineInformationOfOffset(modelOffset);
			final String lineString = document.get(lineRegion.getOffset(), lineRegion.getLength());
			return lineString.substring(modelOffset - lineRegion.getOffset()).replace(";", "").replace(")", "").replace("{", "").replace("{", "").isBlank();
		} catch (final BadLocationException exception) {
			throw new RuntimeException("Failed to check if line suffix is blank", exception);
		}
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

	public boolean abort(String reason) {
		this.paintListener.resetMetrics();
		if (this.job != null) {
			AiCoderActivator.log().info(String.format("Abort job (reason: '%s')", reason));
			this.job.cancel();
			this.job = null;
		}
		if (this.debounceRunnable != null) {
			AiCoderActivator.log().info(String.format("Abort debounce (reason: '%s')", reason));
			Display.getCurrent().timerExec(-1, this.debounceRunnable);
		}
		if (this.context != null) {
			AiCoderActivator.log().info(String.format("Deactivate context (reason: '%s')", reason));
			EclipseUtils.getContextService(this.textEditor).deactivateContext(this.context);
			this.context = null;
		}
		if (this.completion != null) {
			AiCoderActivator.log().info(String.format("Unset completion (reason: '%s')", reason));
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

	private class CaretListenerImplementation implements CaretListener {
		@Override
		public void caretMoved(CaretEvent event) {
			triggerAutocomplete();
		}
	}

	private class DocumentListenerImplementation implements IDocumentListener {
		@Override
		public void documentAboutToBeChanged(DocumentEvent event) {
		}

		@Override
		public void documentChanged(DocumentEvent event) {
			InlineCompletionController.this.changeCounter++;
			abort("Document changed");
		}
	}

	private class SelectionListenerImplementation implements ISelectionChangedListener {
		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			final ISelection selection = event.getSelection();
			if (!(selection instanceof ITextSelection)) {
				return;
			}
			final ITextSelection textSelection = (ITextSelection) selection;
			if (textSelection.getLength() <= 0) {
				return;
			}
			abort("Selection changed");
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
			// TODO update style after font size zoom
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
}
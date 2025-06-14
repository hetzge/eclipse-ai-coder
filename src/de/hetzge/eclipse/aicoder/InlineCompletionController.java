package de.hetzge.eclipse.aicoder;

import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.ICompilationUnit;
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
import org.eclipse.swt.SWT;
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
import org.eclipse.ui.contexts.IContextActivation;
import org.eclipse.ui.texteditor.ITextEditor;

import de.hetzge.eclipse.aicoder.Context.ContextEntry;
import de.hetzge.eclipse.aicoder.Context.RootContextEntry;
import de.hetzge.eclipse.aicoder.Context.TokenCounter;

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
		this.font = font;
		this.completion = null;
		this.context = null;
		this.job = null;
	}

	public void trigger() {

		// Filter JDK
		// Prefer local
		// Unresolved types?!
		// Commenet complete (when newline after comment, calculate completion, optional
		// remove comment)
		// Comment complete "?!" trigger
		// Cache completion

		final int modelOffset = EclipseUtils.getCurrentOffsetInDocument(InlineCompletionController.this.textEditor);
		final IDocument document = this.textViewer.getDocument();

		abort();
		this.job = Job.create("AI inline completion", monitor -> {
			final StringBuilder contextBuilder = new StringBuilder();
			try {
				for (final ICompilationUnit unit : EclipseUtils.getCompilationUnit(this.textEditor).stream().toList()) {
					final RootContextEntry rootContextEntry = RootContextEntry.create(document, unit, modelOffset);
					ContextEntry.apply(contextBuilder, new TokenCounter(30_000), rootContextEntry);
					Display.getDefault().asyncExec(() -> {
						try {
							ContextView.get().ifPresent(view -> {
								view.setRootContextEntry(rootContextEntry);
							});
						} catch (final CoreException exception) {
							throw new RuntimeException(exception);
						}
					});
				}
			} catch (final CoreException | UnsupportedFlavorException | IOException | BadLocationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			try {
				final int widgetLine = getWidgetLine(modelOffset);
				final int widgetOffset = getWidgetOffset(modelOffset);

				final int lineHeight = Display.getDefault().syncCall(() -> InlineCompletionController.this.textViewer.getTextWidget().getLineHeight());
				final int defaultLineSpacing = Display.getDefault().syncCall(() -> InlineCompletionController.this.textViewer.getTextWidget().getLineSpacing());

				if (monitor.isCanceled()) {
					return;
				}
				final String contextString = contextBuilder.toString();
				final String[] contextParts = contextString.split(Context.SuffixContextEntry.FILL_HERE_PLACEHOLDER);
				final String content = LlmUtils.execute(contextParts[0], contextParts[1]);
				if (!content.isBlank()) {
					setup(Completion.create(document, modelOffset, widgetOffset, widgetLine, content, lineHeight, defaultLineSpacing));
				}
			} catch (final IOException exception) {
				throw new CoreException(Status.error("Failed to compute inline completion", exception));
			} catch (final BadLocationException exception) {
				throw new CoreException(Status.error("Failed to compute inline completion", exception));
			}
		});
		this.job.schedule();
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

	private void setup(Completion completion) {
		AiCoderActivator.log().info("Activate context");
		this.context = EclipseUtils.getContextService(this.textEditor).activateContext("de.hetzge.eclipse.codestral.inlineCompletionVisible");
		this.completion = completion;
	}

	public void abort() {
		AiCoderActivator.log().info("Abort");
		if (this.job != null) {
			this.job.cancel();
		}
		if (this.debounceRunnable != null) {
			Display.getCurrent().timerExec(-1, this.debounceRunnable);
		}
		if (this.context != null) {
			AiCoderActivator.log().info("Deactivate context");
			EclipseUtils.getContextService(this.textEditor).deactivateContext(this.context);
			this.context = null;
		}
		this.completion = null;
		this.textViewer.invalidateTextPresentation();
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
		private static final int DEBOUNCE_DELAY_IN_MILLISECONDS = 50;

		@Override
		public void keyPressed(KeyEvent event) {
		}

		@Override
		public void keyReleased(KeyEvent event) {
			abort();
			// Ignore "esc"
			if(event.keyCode == 27) {
				return;
			}
			// Handle "tab" only if no completion context is active
			if(InlineCompletionController.this.context != null && event.keyCode == 9) {
				return;
			}
			if (AiCoderPreferences.isAutocompleteEnabled()) {
				InlineCompletionController.this.debounceRunnable = () -> {
					if (isNoSelectionActive() && isLineSuffixBlank()) {
						trigger();
					}
				};
				Display.getCurrent().timerExec(DEBOUNCE_DELAY_IN_MILLISECONDS, InlineCompletionController.this.debounceRunnable);
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
		@Override
		public void paintControl(PaintEvent event) {
			final Completion completion = InlineCompletionController.this.completion;
			final StyledText widget = InlineCompletionController.this.textViewer.getTextWidget();
			if (completion != null) {
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
							final FontMetrics fontMetrics = event.gc.getFontMetrics();
							final StyleRange styleRange = new StyleRange(completion.widgetOffset(), 1, null, null);
							styleRange.metrics = new GlyphMetrics(fontMetrics.getAscent(), fontMetrics.getDescent(), contentWidth + suffixCharacterWidth);
							widget.setStyleRange(styleRange);
							event.gc.drawText(completion.firstLineSuffixCharacter(), location.x + contentWidth, location.y, false);
						}
					} else {
						event.gc.drawText(line.replace("\t", " ".repeat(InlineCompletionController.this.widget.getTabs())), 0, location.y + i * completion.lineHeight(), true);
					}
				}
			}
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
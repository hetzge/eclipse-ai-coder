package de.hetzge.eclipse.aicoder.inline;

import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.runtime.CoreException;
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
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GlyphMetrics;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.contexts.IContextActivation;
import org.eclipse.ui.texteditor.ITextEditor;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.AiCoderHistoryEntry;
import de.hetzge.eclipse.aicoder.AiCoderHistoryView;
import de.hetzge.eclipse.aicoder.ContextView;
import de.hetzge.eclipse.aicoder.Debouncer;
import de.hetzge.eclipse.aicoder.context.ContextContext;
import de.hetzge.eclipse.aicoder.context.ContextEntry;
import de.hetzge.eclipse.aicoder.context.FillInMiddleContextEntry;
import de.hetzge.eclipse.aicoder.context.RootContextEntry;
import de.hetzge.eclipse.aicoder.llm.LlmPromptTemplates;
import de.hetzge.eclipse.aicoder.llm.LlmResponse;
import de.hetzge.eclipse.aicoder.llm.LlmUtils;
import de.hetzge.eclipse.aicoder.preferences.AiCoderPreferences;
import de.hetzge.eclipse.aicoder.util.EclipseUtils;
import de.hetzge.eclipse.aicoder.util.LambdaExceptionUtils.Runnable_WithExceptions;
import de.hetzge.eclipse.aicoder.util.Utils;

public final class InlineCompletionController {

	private static final Map<ITextViewer, InlineCompletionController> CONTROLLER_BY_VIEWER;
	static {
		CONTROLLER_BY_VIEWER = new ConcurrentHashMap<>();
	}

	public static InlineCompletionController setup(ITextEditor textEditor) {
		final ITextViewer textViewer = EclipseUtils.getTextViewer(textEditor);
		return CONTROLLER_BY_VIEWER.computeIfAbsent(textViewer, ignore -> {
			final InlineCompletionController controller = new InlineCompletionController(textViewer, textEditor);
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
	private List<InlineCompletion> completions;
	private IContextActivation context;
	private Job job;
	private long changeCounter;
	private long lastChangeCounter;
	private final Debouncer debouncer;
	private boolean abortDisabled;
	private SuggestionPopupDialog suggestionPopupDialog;
	private Suggestion suggestion;

	private InlineCompletionController(ITextViewer textViewer, ITextEditor textEditor) {
		this.textViewer = textViewer;
		this.textEditor = textEditor;
		this.widget = textViewer.getTextWidget();
		this.spacingProvider = new StyledTextLineSpacingProviderImplementation();
		this.documentListener = new DocumentListenerImplementation();
		this.paintListener = new PaintListenerImplementation();
		this.painter = new PainterImplementation();
		this.selectionListener = new SelectionListenerImplementation();
		this.caretListener = new CaretListenerImplementation();
		this.completions = new ArrayList<>();
		this.context = null;
		this.job = null;
		this.changeCounter = 0;
		this.lastChangeCounter = 0;
		this.debouncer = new Debouncer(Display.getDefault(), AiCoderPreferences::getDebounceDuration);
		this.abortDisabled = false;
		this.suggestionPopupDialog = null;
		this.suggestion = null;
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
			if (!hasSelection() && isAutocompleteAllowed()) {
				trigger();
			}
		});
	}

	public void trigger() {
		AiCoderActivator.log().info("Trigger");
		final long startTime = System.currentTimeMillis();
		abort("Trigger");
		final StyledText widget = InlineCompletionController.this.textViewer.getTextWidget();
		final int lineHeight = widget.getLineHeight();
		final int defaultLineSpacing = widget.getLineSpacing();
		this.job = Job.create("AI inline completion", monitor -> {
			String contextString = "";
			try {
				final int modelOffset = EclipseUtils.getCurrentOffsetInDocument(InlineCompletionController.this.textEditor);
				final IDocument document = this.textViewer.getDocument();
				final RootContextEntry rootContextEntry = RootContextEntry.create(document, this.textEditor.getEditorInput(), modelOffset);
				if (monitor.isCanceled()) {
					addHistoryEntry("Aborted");
					return;
				}
				contextString = ContextEntry.apply(rootContextEntry, new ContextContext());
				// IMPORTANT: DO this after ContextEntry.apply(...)
				updateContextView(rootContextEntry);
				if (monitor.isCanceled()) {
					addHistoryEntry("Aborted");
					return;
				}
				final String[] contextParts = contextString.split(FillInMiddleContextEntry.FILL_HERE_PLACEHOLDER);
				final String prefix = contextParts[0];
				final String suffix = contextParts.length > 1 ? contextParts[1] : "";
				final boolean hasSelection = hasSelection();
				final String selectionText = getSelectionText();
				final long llmStartTime = System.currentTimeMillis();
				LlmResponse llmResponse;
				if (hasSelection) {
					// TODO context
					// TODO filetype
					llmResponse = LlmUtils.execute(LlmPromptTemplates.changeCodePrompt("java", selectionText, "Fix/complete the code"), null);
				} else {
					llmResponse = LlmUtils.execute(prefix, suffix);
				}
				final String content = llmResponse.getContent();
				final long llmDuration = System.currentTimeMillis() - llmStartTime;
				final int currentModelOffset = EclipseUtils.getCurrentOffsetInDocument(InlineCompletionController.this.textEditor);
				final boolean isMultilineContent = content.contains("\n");
				final boolean isBlank = content.isBlank();
				final boolean isMoved = currentModelOffset != modelOffset;
				final boolean isSame = hasSelection
						? false
						: isMultilineContent && suffix.replaceAll("\\s", "").startsWith(content.replaceAll("\\s", ""));
				if (monitor.isCanceled()) {
					addHistoryEntry("Aborted");
					return;
				}
				if (!isBlank && !isMoved && !isSame) {
					if (hasSelection) {
						// TODO History content, type
						setup(new Suggestion(
								Utils.stripCodeMarkdownTags(content),
								modelOffset,
								selectionText.length(),
								getWidgetLine(modelOffset) + (int) selectionText.lines().count() - 1,
								Math.max((int) Utils.stripCodeMarkdownTags(content).lines().count() - (int) selectionText.lines().count(), 0) + 2)); // + 2 for popup toolbar
					} else {
						setup(List.of(InlineCompletion.create(
								document,
								modelOffset,
								getWidgetOffset(modelOffset),
								getWidgetLine(modelOffset),
								content,
								lineHeight,
								defaultLineSpacing)));
					}
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
				final long duration = System.currentTimeMillis() - startTime;
				addHistoryEntry(contextString, content, status, duration, llmDuration, llmResponse);
			} catch (final IOException | BadLocationException | UnsupportedFlavorException exception) {
				AiCoderActivator.log().error("AI Coder completion failed", exception);
				final long duration = System.currentTimeMillis() - startTime;
				final String stacktrace = Utils.getStacktraceString(exception);
				addHistoryEntry(contextString, stacktrace, "Error: " + Optional.ofNullable(exception.getMessage()).orElse("-"), duration, 0, null);
			}
		});
		this.job.schedule();
	}

	private boolean hasSelection() {
		return Display.getDefault().syncCall(() -> InlineCompletionController.this.textViewer.getSelectedRange().y > 0);
	}

	private String getSelectionText() {
		return Display.getDefault().syncCall(() -> InlineCompletionController.this.textViewer.getSelectionProvider().getSelection() instanceof final ITextSelection textSelection ? textSelection.getText() : "");
	}

	private void unsetSelection() {
		final int selectionOffset = Display.getDefault().syncCall(() -> InlineCompletionController.this.textViewer.getSelectionProvider().getSelection() instanceof final ITextSelection textSelection ? textSelection.getOffset() : 0);
		Display.getDefault().syncExec(() -> InlineCompletionController.this.textViewer.setSelectedRange(selectionOffset, 0));
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

	private void addHistoryEntry(String status) {
		addHistoryEntry("", "", status, 0, 0, null);
	}

	private void addHistoryEntry(String input, String output, String status, long duration, long llmDuration, LlmResponse llmResponse) {
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
					llmResponse != null ? llmResponse.getInputTokens() : 0,
					llmResponse != null ? llmResponse.getOutputTokens() : 0,
					duration,
					llmDuration,
					llmResponse != null ? llmResponse.getPlainResponse() : "");

			AiCoderHistoryView.get().ifPresent(view -> {
				view.addHistoryEntry(entry);
			});
		});
	}

	private void setup(List<InlineCompletion> completions) {
		AiCoderActivator.log().info("Activate context (completion)");
		this.completions = new ArrayList<>(completions);
		setupContext();
	}

	private void setup(Suggestion suggestion) {
		AiCoderActivator.log().info("Activate context (content)");
		this.suggestion = suggestion;
		setupContext();
		Display.getDefault().syncExec(() -> {
			final Runnable acceptListener = () -> accept();
			final Runnable rejectListener = () -> abort("Dismiss");
			this.suggestionPopupDialog = new SuggestionPopupDialog(this.widget, suggestion, acceptListener, rejectListener);
			this.suggestionPopupDialog.open();
			unsetSelection();
		});
	}

	private void setupContext() {
		Display.getDefault().syncExec(() -> {
			this.context = EclipseUtils.getContextService(this.textEditor).activateContext("de.hetzge.eclipse.codestral.inlineCompletionVisible");
		});
	}

	public boolean abort(String reason) {
		if (this.abortDisabled) {
			return false;
		}
		if (this.suggestionPopupDialog != null) {
			AiCoderActivator.log().info(String.format("Close suggestion popup dialog (reason: '%s')", reason));
			this.suggestionPopupDialog.close();
			this.suggestionPopupDialog = null;
		}
		if (this.suggestion != null) {
			AiCoderActivator.log().info(String.format("Unset suggestion (reason: '%s')", reason));
			this.suggestion = null;
			this.paintListener.resetMetrics();
		}
		if (this.job != null) {
			AiCoderActivator.log().info(String.format("Abort job (reason: '%s')", reason));
			this.job.cancel();
			this.job = null;
		}
		if (this.context != null) {
			AiCoderActivator.log().info(String.format("Deactivate context (reason: '%s')", reason));
			EclipseUtils.getContextService(this.textEditor).deactivateContext(this.context);
			this.context = null;
		}
		if (!this.completions.isEmpty()) {
			AiCoderActivator.log().info(String.format("Unset completions (reason: '%s')", reason));
			this.completions.clear();
			this.paintListener.resetMetrics();
			return true;
		}
		return false;
	}

	private synchronized <T extends Exception> void executeThenAbort(Runnable_WithExceptions<T> runnable, String reason) throws T {
		try {
			this.abortDisabled = true;
			runnable.run();
		} finally {
			this.abortDisabled = false;
			abort(reason);
		}
	}

	public void accept() {
		acceptInlineCompletion();
		acceptSuggestion();
	}

	private void acceptInlineCompletion() {
		if (this.completions.isEmpty()) {
			return;
		}
		try {
			executeThenAbort(() -> { // prevent early abort by document change
				final IDocument document = this.textViewer.getDocument();
				int offset = 0;
				// TODO do as one replace/change?! is this possible?
				for (final InlineCompletion completion : this.completions) {
					final int replaceOffset = completion.modelRegion().getOffset() + offset;
					final int replaceLength = completion.modelRegion().getLength();
					document.replace(replaceOffset, replaceLength, completion.content());
					offset += (completion.content().length() - completion.modelRegion().getLength());
					this.textViewer.setSelectedRange(completion.modelRegion().getOffset() + completion.content().length(), 0);
				}
				AiCoderHistoryView.get().ifPresent(view -> {
					view.setLatestAccepted();
				});
			}, "Accepted");
		} catch (final BadLocationException exception) {
			throw new RuntimeException("Failed to accept inline completion", exception);
		}
	}

	private void acceptSuggestion() {
		if (this.suggestion == null) {
			return;
		}
		try {
			final IDocument document = this.textViewer.getDocument();
			final int offset = this.suggestion.modelOffset();
			final int length = this.suggestion.originalLength();
			document.replace(offset, length, this.suggestion.content());
			this.textViewer.setSelectedRange(offset + length, 0);
		} catch (final BadLocationException exception) {
			throw new RuntimeException("Failed to accept suggestion", exception);
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
			final List<InlineCompletion> completions = InlineCompletionController.this.completions;
			for (final InlineCompletion completion : completions) {
				if (completion.widgetLineIndex() == lineIndex) {
					return completion.lineSpacing();
				}
			}
			final Suggestion suggestion = InlineCompletionController.this.suggestion;
			if (suggestion != null && suggestion.widgetLastLine() == lineIndex) {
				return suggestion.additionalLines() * InlineCompletionController.this.widget.getLineHeight();
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
			final StyledText widget = InlineCompletionController.this.textViewer.getTextWidget();
			final Font font = widget.getFont();
			for (final InlineCompletion completion : InlineCompletionController.this.completions) {
				final Point location = widget.getLocationAtOffset(completion.widgetOffset());
				final List<String> lines = completion.content().lines().toList();
				event.gc.setBackground(new Color(200, 255, 200));
				event.gc.setForeground(widget.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
				event.gc.setFont(font);
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
						event.gc.drawText(line.replace("\t", " ".repeat(InlineCompletionController.this.widget.getTabs())), -widget.getHorizontalPixel(), location.y + i * completion.lineHeight(), true);
					}
				}
			}
		}

		private boolean needMetricUpdate(final StyleRange styleRange, final int metricWidth) {
			return styleRange == null || styleRange.metrics == null || styleRange.metrics.width != metricWidth;
		}

		private void updateMetrics(PaintEvent event, InlineCompletion completion, StyledText widget, int metricWidth) {
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
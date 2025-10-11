package de.hetzge.eclipse.aicoder.inline;

import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.ICompilationUnit;
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
import de.hetzge.eclipse.aicoder.CompletionMode;
import de.hetzge.eclipse.aicoder.ContextView;
import de.hetzge.eclipse.aicoder.Debouncer;
import de.hetzge.eclipse.aicoder.context.ContextContext;
import de.hetzge.eclipse.aicoder.context.ContextEntry;
import de.hetzge.eclipse.aicoder.context.FillInMiddleContextEntry;
import de.hetzge.eclipse.aicoder.context.RootContextEntry;
import de.hetzge.eclipse.aicoder.history.AiCoderHistoryEntry;
import de.hetzge.eclipse.aicoder.history.AiCoderHistoryView;
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
	private AiCoderHistoryEntry historyEntry;

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
		this.historyEntry = null;
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
			if (!EclipseUtils.hasSelection(this.textViewer) && isAutocompleteAllowed()) {
				trigger(null);
			}
		});
	}

	public void trigger(String instruction) {
		AiCoderActivator.log().info("Trigger");
		final long startTime = System.currentTimeMillis();
		abort("Trigger");
		final StyledText widget = InlineCompletionController.this.textViewer.getTextWidget();
		final int lineHeight = widget.getLineHeight();
		final int defaultLineSpacing = widget.getLineSpacing();
		final IEditorInput editorInput = this.textEditor.getEditorInput();
		final String filePath = editorInput.getName();
		final boolean hasSelection = EclipseUtils.hasSelection(this.textViewer);
		final CompletionMode mode = hasSelection
				? CompletionMode.EDIT
				: instruction == null
						? CompletionMode.INLINE
						: CompletionMode.GENERATE;
		final AiCoderHistoryEntry historyEntry = new AiCoderHistoryEntry(mode, filePath, this.textViewer.getDocument().get());
		this.historyEntry = historyEntry;
		updateHistoryEntry(historyEntry);
		this.job = Job.create("AI completion", monitor -> {
			String prompt = "";
			LlmResponse llmResponse = null;
			try {
				final int modelOffset = EclipseUtils.getCurrentOffsetInDocument(InlineCompletionController.this.textEditor);
				final IDocument document = this.textViewer.getDocument();
				final RootContextEntry rootContextEntry = RootContextEntry.create(document, this.textEditor.getEditorInput(), modelOffset);
				final String contextString = ContextEntry.apply(rootContextEntry, new ContextContext());
				// IMPORTANT: DO this after ContextEntry.apply(...)
				updateContextView(rootContextEntry);
				final String[] contextParts = contextString.split(FillInMiddleContextEntry.FILL_HERE_PLACEHOLDER);
				final String prefix = contextParts[0];
				final String suffix = contextParts.length > 1 ? contextParts[1] : "";
				final String selectionText = EclipseUtils.getSelectionText(this.textViewer);
				if (mode == CompletionMode.EDIT || mode == CompletionMode.GENERATE) {
					final String fileType = EclipseUtils.getFileExtension(this.textEditor.getEditorInput());
					final String systemPrompt = hasSelection
							? LlmPromptTemplates.changeCodeSystemPrompt()
							: LlmPromptTemplates.generateCodeSystemPrompt();
					final String effectiveInstruction = instruction != null ? instruction : "Fix/complete the code";
					prompt = hasSelection
							? LlmPromptTemplates.changeCodePrompt(fileType, selectionText, effectiveInstruction, prefix, suffix)
							: LlmPromptTemplates.generateCodePrompt(effectiveInstruction, prefix, suffix);
					llmResponse = LlmUtils.executeGenerate(systemPrompt, prompt);
				} else if (mode == CompletionMode.INLINE) {
					prompt = prefix + "<!!!>" + suffix;
					llmResponse = LlmUtils.executeFillInTheMiddle(prefix, suffix);
				} else {
					throw new IllegalStateException("Unknown completion mode: " + mode);
				}
				String content = Utils.stripCodeMarkdownTags(llmResponse.getContent());
				final int currentModelOffset = EclipseUtils.getCurrentOffsetInDocument(InlineCompletionController.this.textEditor);
				final boolean isMultilineContent = content.contains("\n");
				final boolean isBlank = content.isBlank();
				final boolean isMoved = currentModelOffset != modelOffset;
				final boolean isSame = hasSelection
						? false
						: isMultilineContent && suffix.replaceAll("\\s", "").startsWith(content.replaceAll("\\s", ""));
				if (monitor.isCanceled()) {
					historyEntry.setStatus("Aborted");
					updateHistoryEntry(historyEntry);
					return;
				}
				if (!isBlank && !isMoved && !isSame) {
					if (mode == CompletionMode.EDIT) {
						final int newLineCount = (int) content.lines().count();
						final int oldLineCount = (int) selectionText.lines().count();
						setup(new Suggestion(
								content,
								modelOffset,
								selectionText.length(),
								EclipseUtils.getWidgetLine(this.textViewer, modelOffset) + oldLineCount - 1,
								newLineCount,
								oldLineCount,
								Math.max(newLineCount - oldLineCount, 0)));
					} else if (mode == CompletionMode.INLINE || mode == CompletionMode.GENERATE) {
						// TODO validate if this is working
						if (isMultilineContent) {
							final long lineCount = content.lines().count();
							final List<String> contentLines = content.lines().limit(lineCount).toList();
							final List<String> suffixLines = suffix.lines().limit(lineCount).toList();
							if (contentLines.size() == suffixLines.size()) {
								for (int i = 0; i < lineCount; i++) {
									final String contentLine = contentLines.get((int) (lineCount - 1 - i)).replaceAll("\\s", " ");
									final String suffixLine = suffixLines.get((int) (lineCount - 1 - i)).replaceAll("\\s", " ");
									if (!Objects.equals(contentLine, suffixLine)) {
										AiCoderActivator.log().info(String.format("Remove %d equal suffix lines", i));
										content = content.lines().limit(lineCount - i).collect(Collectors.joining("\n"));
										break;
									}
								}
							}
						}

						setup(InlineCompletion.create(
								document,
								modelOffset,
								EclipseUtils.getWidgetOffset(this.textViewer, modelOffset),
								EclipseUtils.getWidgetLine(this.textViewer, modelOffset),
								content,
								lineHeight,
								defaultLineSpacing));
					} else {
						throw new IllegalStateException("Unknown completion mode: " + mode);
					}
				}
				final long duration = System.currentTimeMillis() - startTime;
				historyEntry.setStatus(calculateStatus(isBlank, isMoved, isSame));
				historyEntry.setDurationMs(duration);
				historyEntry.setLlmDurationMs(llmResponse.getDuration().toMillis());
				historyEntry.setPlainLlmResponse(llmResponse.getPlainResponse());
				historyEntry.setModelLabel(llmResponse.getLlmModelOption().getLabel());
				historyEntry.setInputTokenCount(llmResponse.getInputTokens());
				historyEntry.setOutputTokenCount(llmResponse.getOutputTokens());
				historyEntry.setInput(prompt);
				historyEntry.setOutput(content);
				updateHistoryEntry(historyEntry);
			} catch (final IOException | BadLocationException | UnsupportedFlavorException exception) {
				AiCoderActivator.log().error("AI Coder completion failed", exception);
				final long duration = System.currentTimeMillis() - startTime;
				final String stacktrace = Utils.getStacktraceString(exception);
				historyEntry.setStatus("Error: " + Optional.ofNullable(exception.getMessage()).orElse("-"));
				historyEntry.setDurationMs(duration);
				historyEntry.setLlmDurationMs(llmResponse != null ? llmResponse.getDuration().toMillis() : 0);
				historyEntry.setPlainLlmResponse(stacktrace);
				historyEntry.setModelLabel(llmResponse != null ? llmResponse.getLlmModelOption().getLabel() : null);
				historyEntry.setInputTokenCount(llmResponse != null ? llmResponse.getInputTokens() : 0);
				historyEntry.setOutputTokenCount(llmResponse != null ? llmResponse.getOutputTokens() : 0);
				historyEntry.setInput(prompt);
				historyEntry.setOutput(stacktrace);
				updateHistoryEntry(historyEntry);
			}
		});
		this.job.schedule();
	}

	private String calculateStatus(boolean isBlank, boolean isMoved, boolean isSame) {
		if (isMoved) {
			return "Moved";
		} else if (isBlank) {
			return "Blank";
		} else if (isSame) {
			return "Same";
		} else {
			return "Generated";
		}
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

	private void updateContextView(RootContextEntry rootContextEntry) {
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

	private void setup(InlineCompletion completion) {
		AiCoderActivator.log().info("Activate context (completion)");
		this.completions = new ArrayList<>(List.of(completion));
		setupContext();
		redraw();
	}

	private void setup(Suggestion suggestion) {
		AiCoderActivator.log().info("Activate context (content)");
		this.suggestion = suggestion;
		setupContext();
		Display.getDefault().syncExec(() -> {
			this.textViewer.getTextWidget().redraw(); // windows needs this
			final Runnable acceptListener = () -> {
				accept();
			};
			final Runnable rejectListener = () -> {
				abort("Dismiss");
				AiCoderHistoryView.get().ifPresent(view -> {
					view.setLatestRejected();
				});
			};
			this.suggestionPopupDialog = new SuggestionPopupDialog(this.textViewer, suggestion, acceptListener, rejectListener);
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
			this.textEditor.setFocus();
		}
		if (this.suggestion != null) {
			AiCoderActivator.log().info(String.format("Unset suggestion (reason: '%s')", reason));
			this.suggestion = null;
			this.paintListener.resetMetrics();
			redraw();
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
			redraw();
			return true;
		}
		if (this.historyEntry != null) {
			AiCoderActivator.log().info(String.format("Unset history entry (reason: '%s')", reason));
			this.historyEntry = null;
		}
		return false;
	}

	private void redraw() {
		Display.getDefault().syncExec(() -> {
			this.textViewer.getTextWidget().redraw(); // windows needs this
		});
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
		// Store in variable because accept trigger abort
		final AiCoderHistoryEntry historyEntry = this.historyEntry;
		acceptInlineCompletion();
		acceptSuggestion();
		if (AiCoderPreferences.isCleanupCodeOnApplyEnabled()) {
			AiCoderActivator.log().info("Trigger code cleanup on apply");
			final Optional<ICompilationUnit> compilationUnitOptional = EclipseUtils.getCompilationUnit(this.textEditor.getEditorInput());
			if (compilationUnitOptional.isPresent()) {
				final ICompilationUnit compilationUnit = compilationUnitOptional.get();
				try {
					AiCoderCodeCleanupUtils.triggerSaveActions(compilationUnit);
				} catch (OperationCanceledException | CoreException exception) {
					AiCoderActivator.log().error("Failed to cleanup code", exception);
				}
			}
		}
		historyEntry.setContent(this.textViewer.getDocument().get());
		this.textEditor.setFocus();
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
			AiCoderHistoryView.get().ifPresent(view -> {
				view.setLatestAccepted();
			});
		} catch (final BadLocationException exception) {
			throw new RuntimeException("Failed to accept suggestion", exception);
		}
	}

	private void updateHistoryEntry(AiCoderHistoryEntry historyEntry) {
		AiCoderHistoryView.get().ifPresent(view -> {
			Display.getDefault().asyncExec(() -> {
				view.addHistoryEntry(historyEntry);
			});
		});
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
				return (suggestion.additionalLines() + 2) * InlineCompletionController.this.widget.getLineHeight(); // +2 for the buttons
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
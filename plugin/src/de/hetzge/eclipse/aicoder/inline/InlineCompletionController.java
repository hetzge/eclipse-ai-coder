package de.hetzge.eclipse.aicoder.inline;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IPaintPositionManager;
import org.eclipse.jface.text.IPainter;
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
import de.hetzge.eclipse.aicoder.history.HistoryStatus;
import de.hetzge.eclipse.aicoder.llm.LlmPromptTemplates;
import de.hetzge.eclipse.aicoder.llm.LlmResponse;
import de.hetzge.eclipse.aicoder.llm.LlmUtils;
import de.hetzge.eclipse.aicoder.preferences.AiCoderPreferences;
import de.hetzge.eclipse.aicoder.util.EclipseUtils;
import de.hetzge.eclipse.aicoder.util.LambdaExceptionUtils.Runnable_WithExceptions;
import de.hetzge.eclipse.aicoder.util.Utils;

// TODO spacing bug while completion is open
// TODO overlay moves while scrolling bug
// TODO regenerate button in inline suggestion
// TODO last input tokens status bar information (to keep an eye on token usage), use colors to indicate if it is too much

public final class InlineCompletionController {

	private static final ISchedulingRule COMPLETION_JOB_RULE = new ISchedulingRule() {
		@Override
		public boolean contains(ISchedulingRule other) {
			return this == other;
		}

		@Override
		public boolean isConflicting(ISchedulingRule other) {
			return this == other;
		}
	};

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
	private InlineCompletion completion;
	private IContextActivation context;
	private Job job;
	private long changeCounter;
	private long lastChangeCounter;
	private final Debouncer debouncer;
	private boolean abortDisabled;
	private SuggestionPopupDialog suggestionPopupDialog;
	private Suggestion suggestion;
	private Future<LlmResponse> llmResponseFuture;

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
		this.completion = null;
		this.context = null;
		this.job = null;
		this.changeCounter = 0;
		this.lastChangeCounter = 0;
		this.debouncer = new Debouncer(Display.getDefault(), AiCoderPreferences::getDebounceDuration);
		this.abortDisabled = false;
		this.suggestionPopupDialog = null;
		this.suggestion = null;
		this.llmResponseFuture = null;
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
			if (!EclipseUtils.hasSelection(this.textViewer)) {
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
		CompletionMode mode;
		if (hasSelection) {
			if (instruction == null) {
				mode = CompletionMode.QUICK_FIX;
			} else {
				mode = CompletionMode.EDIT;
			}
		} else {
			if (instruction == null) {
				mode = CompletionMode.INLINE;
			} else {
				mode = CompletionMode.GENERATE;
			}
		}
		final AiCoderHistoryEntry historyEntry = new AiCoderHistoryEntry(mode, filePath, this.textViewer.getDocument().get());
		this.job = new Job("AI completion") {

			ITextViewer textViewer = InlineCompletionController.this.textViewer;
			ITextEditor textEditor = InlineCompletionController.this.textEditor;

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				String prompt = "";
				LlmResponse llmResponse = null;
				try {
					updateHistoryEntry(historyEntry);
					final int modelOffset = EclipseUtils.getCurrentOffsetInDocument(InlineCompletionController.this.textEditor);
					final IDocument document = this.textViewer.getDocument();
					if (monitor.isCanceled()) {
						historyEntry.setStatus(HistoryStatus.CANCELED);
						updateHistoryEntry(historyEntry);
						return Status.CANCEL_STATUS;
					}
					AiCoderActivator.log().info("Calculate context");
					final RootContextEntry rootContextEntry = RootContextEntry.create(document, this.textEditor.getEditorInput(), modelOffset);
					final String contextString = ContextEntry.apply(rootContextEntry, new ContextContext());
					// IMPORTANT: DO this after ContextEntry.apply(...)
					updateContextView(rootContextEntry);
					if (monitor.isCanceled()) {
						historyEntry.setStatus(HistoryStatus.CANCELED);
						updateHistoryEntry(historyEntry);
						return Status.CANCEL_STATUS;
					}
					final String[] contextParts = contextString.split(FillInMiddleContextEntry.FILL_HERE_PLACEHOLDER);
					final String prefix = contextParts[0];
					final String suffix = contextParts.length > 1 ? contextParts[1] : "";
					final String selectionText = EclipseUtils.getSelectionText(this.textViewer);
					if (mode == CompletionMode.EDIT || mode == CompletionMode.GENERATE || mode == CompletionMode.QUICK_FIX) {
						final String fileType = EclipseUtils.getFileExtension(this.textEditor.getEditorInput());
						final String systemPrompt = hasSelection
								? AiCoderPreferences.getChangeCodeSystemPrompt()
								: AiCoderPreferences.getGenerateCodeSystemPrompt();
						final String effectiveInstruction = instruction != null ? instruction : AiCoderPreferences.getQuickFixPrompt();
						prompt = hasSelection
								? LlmPromptTemplates.changeCodePrompt(fileType, selectionText, effectiveInstruction, prefix, suffix)
								: LlmPromptTemplates.generateCodePrompt(effectiveInstruction, prefix, suffix);
						if (mode == CompletionMode.EDIT) {
							InlineCompletionController.this.llmResponseFuture = LlmUtils.executeEdit(systemPrompt, prompt);
						} else if (mode == CompletionMode.QUICK_FIX) {
							InlineCompletionController.this.llmResponseFuture = LlmUtils.executeQuickFix(systemPrompt, prompt);
						} else {
							InlineCompletionController.this.llmResponseFuture = LlmUtils.executeGenerate(systemPrompt, prompt);
						}
					} else if (mode == CompletionMode.INLINE) {
						prompt = prefix + "<!!!>" + suffix;
						InlineCompletionController.this.llmResponseFuture = LlmUtils.executeFillInTheMiddle(prefix, suffix);
					} else {
						throw new IllegalStateException("Unknown completion mode: " + mode);
					}
					AiCoderActivator.log().info("Wait for LLM response");
					try {
						llmResponse = InlineCompletionController.this.llmResponseFuture.get();
					} catch (final ExecutionException exception) {
						if (exception.getCause() instanceof CancellationException) {
							historyEntry.setStatus(HistoryStatus.CANCELED);
							updateHistoryEntry(historyEntry);
							return Status.CANCEL_STATUS;
						}
						throw exception;
					}
					if (llmResponse.isError()) {
						historyEntry.setStatus(HistoryStatus.ERROR);
						historyEntry.setPlainLlmResponse(llmResponse.getPlainResponse());
						historyEntry.setModelLabel(llmResponse.getLlmModelOption().getLabel());
						historyEntry.setInput(prompt);
						historyEntry.setOutput(llmResponse.getContent());
						updateHistoryEntry(historyEntry);
						return Status.OK_STATUS;
					}
					final String content = Utils.stripCodeMarkdownTags(llmResponse.getContent());
					final int currentModelOffset = EclipseUtils.getCurrentOffsetInDocument(InlineCompletionController.this.textEditor);
					final boolean isMultilineContent = content.contains("\n");
					final boolean isBlank = content.isBlank();
					final boolean isMoved = currentModelOffset != modelOffset;
					final boolean isSame = hasSelection
							? false
							: isMultilineContent && suffix.replaceAll("\\s", "").startsWith(content.replaceAll("\\s", ""));
					if (monitor.isCanceled()) {
						historyEntry.setStatus(HistoryStatus.CANCELED);
						updateHistoryEntry(historyEntry);
						return Status.CANCEL_STATUS;
					}
					if (!isBlank && !isMoved && !isSame) {
						if (mode == CompletionMode.EDIT || mode == CompletionMode.QUICK_FIX) {
							final int newLineCount = (int) content.lines().count();
							final int oldLineCount = (int) selectionText.lines().count();
							setup(new Suggestion(
									historyEntry,
									content,
									modelOffset,
									selectionText.length(),
									EclipseUtils.getWidgetLine(this.textViewer, modelOffset) + oldLineCount - 1,
									newLineCount,
									oldLineCount,
									Math.max(newLineCount - oldLineCount, 0)));
						} else if (mode == CompletionMode.INLINE || mode == CompletionMode.GENERATE) {
							setup(InlineCompletion.create(
									historyEntry,
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
					return Status.OK_STATUS;
				} catch (final Exception exception) {
					AiCoderActivator.log().error("AI Coder completion failed", exception);
					final long duration = System.currentTimeMillis() - startTime;
					final String stacktrace = Utils.getStacktraceString(exception);
					historyEntry.setStatus(HistoryStatus.ERROR);
					historyEntry.setDurationMs(duration);
					historyEntry.setLlmDurationMs(0);
					historyEntry.setPlainLlmResponse(llmResponse != null ? llmResponse.getPlainResponse() : "");
					historyEntry.setModelLabel(null);
					historyEntry.setInputTokenCount(0);
					historyEntry.setOutputTokenCount(0);
					historyEntry.setInput(prompt);
					historyEntry.setOutput((llmResponse != null ? llmResponse.getContent() : "") + stacktrace);
					updateHistoryEntry(historyEntry);
					return Status.OK_STATUS;
				}
			}

			@Override
			protected void canceling() {
				cancelHttpRequest();
			}
		};
		this.job.setRule(COMPLETION_JOB_RULE);
		this.job.schedule();
	}

	private void cancelHttpRequest() {
		AiCoderActivator.log().info("Canceling");
		if (this.llmResponseFuture != null) {
			AiCoderActivator.log().info("Cancel LLM response future");
			this.llmResponseFuture.cancel(true);
			this.llmResponseFuture = null;
		}
	}

	private HistoryStatus calculateStatus(boolean isBlank, boolean isMoved, boolean isSame) {
		if (isMoved) {
			return HistoryStatus.MOVED;
		} else if (isBlank) {
			return HistoryStatus.BLANK;
		} else if (isSame) {
			return HistoryStatus.EQUAL;
		} else {
			return HistoryStatus.GENERATED;
		}
	}

	private void unsetSelection() {
		final int selectionOffset = Display.getDefault().syncCall(() -> InlineCompletionController.this.textViewer.getSelectionProvider().getSelection() instanceof final ITextSelection textSelection ? textSelection.getOffset() : 0);
		Display.getDefault().syncExec(() -> InlineCompletionController.this.textViewer.setSelectedRange(selectionOffset, 0));
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
		this.completion = completion;
		setupContext();
		redraw();
	}

	private void setup(Suggestion suggestion) {
		AiCoderActivator.log().info("Activate context (content)");
		this.suggestion = suggestion;
		setupContext();
		redraw();
		Display.getDefault().syncExec(() -> {
			this.suggestionPopupDialog = new SuggestionPopupDialog(this.textViewer, suggestion);
			this.suggestionPopupDialog.open();
			this.suggestionPopupDialog.getShell().addDisposeListener(event -> {
				final int returnCode = InlineCompletionController.this.suggestionPopupDialog.getReturnCode();
				AiCoderActivator.log().info(String.format("Suggestion popup dialog returned with code: %d", returnCode));
				if (returnCode == SuggestionPopupDialog.ACCEPT_RETURN_CODE) {
					accept();
				} else {
					abort("Dismiss");
				}
				unsetSelection();
			});
		});
	}

	private void setupContext() {
		Display.getDefault().syncExec(() -> {
			this.context = EclipseUtils.getContextService(this.textEditor).activateContext("de.hetzge.eclipse.codestral.inlineCompletionVisible");
		});
	}

	public void abort(String reason) {
		if (this.abortDisabled) {
			return;
		}
		if (this.llmResponseFuture != null) {
			AiCoderActivator.log().info(String.format("Cancel LLM response future (reason: '%s')", reason));
			this.llmResponseFuture.cancel(true);
			this.llmResponseFuture = null;
		}
		if (this.suggestionPopupDialog != null) {
			AiCoderActivator.log().info(String.format("Close suggestion popup dialog (reason: '%s')", reason));
			this.suggestionPopupDialog.close();
			this.suggestionPopupDialog = null;
			this.textEditor.setFocus();
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
		if (this.suggestion != null) {
			AiCoderActivator.log().info(String.format("Unset suggestion (reason: '%s')", reason));
			if (this.suggestion.historyEntry().getStatus() == HistoryStatus.GENERATED) {
				this.suggestion.historyEntry().setStatus(HistoryStatus.REJECTED);
			}
			this.suggestion = null;
			AiCoderHistoryView.get().ifPresent(AiCoderHistoryView::refresh);
			this.paintListener.resetMetrics();
		}
		if (this.completion != null) {
			AiCoderActivator.log().info(String.format("Unset completions (reason: '%s')", reason));
			if (this.completion.historyEntry().getStatus() == HistoryStatus.GENERATED) {
				this.completion.historyEntry().setStatus(HistoryStatus.REJECTED);
			}
			this.completion = null;
			AiCoderHistoryView.get().ifPresent(AiCoderHistoryView::refresh);
			this.paintListener.resetMetrics();
		}
	}

	private void redraw() {
		Display.getDefault().syncExec(() -> {
			// windows needs this ?! (TODO investigate if this is still needed, or if possible to only redraw affected lines)
			this.textViewer.getTextWidget().redraw();
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
		if (this.completion != null) {
			acceptInlineCompletion();
		}
		if (this.suggestion != null) {
			acceptSuggestion();
		}
		if (AiCoderPreferences.isCleanupCodeOnApplyEnabled()) {
			// TODO only cleanup if syntax is complete
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
	}

	private void acceptInlineCompletion() {
		try {
			executeThenAbort(() -> { // prevent early abort by document change
				this.completion.applyTo(this.textViewer.getDocument());
				this.textViewer.setSelectedRange(this.completion.modelRegion().getOffset() + this.completion.content().length(), 0);
				this.completion.historyEntry().setStatus(HistoryStatus.ACCEPTED);
				this.completion.historyEntry().setContent(this.textViewer.getDocument().get());
				AiCoderHistoryView.get().ifPresent(AiCoderHistoryView::refresh);
			}, "Accepted");
		} catch (final BadLocationException exception) {
			throw new RuntimeException("Failed to accept inline completion", exception);
		}
	}

	private void acceptSuggestion() {
		try {
			executeThenAbort(() -> { // prevent early abort by document change
				this.suggestion.applyTo(this.textViewer.getDocument());
				this.textViewer.setSelectedRange(this.suggestion.modelOffset() + this.suggestion.content().length(), 0);
				this.suggestion.historyEntry().setStatus(HistoryStatus.ACCEPTED);
				this.suggestion.historyEntry().setContent(this.textViewer.getDocument().get());
				AiCoderHistoryView.get().ifPresent(AiCoderHistoryView::refresh);
			}, "Accepted");
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
			final InlineCompletion completion = InlineCompletionController.this.completion;
			if (completion != null && completion.widgetLineIndex() == lineIndex) {
				return completion.lineSpacing();
			}
			final Suggestion suggestion = InlineCompletionController.this.suggestion;
			final SuggestionPopupDialog suggestionPopupDialog = InlineCompletionController.this.suggestionPopupDialog;
			if (suggestionPopupDialog != null && suggestion != null && suggestion.widgetLastLine() == lineIndex) {
				return (suggestionPopupDialog.getLineCount() - suggestion.oldLines() + 2) * InlineCompletionController.this.widget.getLineHeight(); // +2 for the buttons
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
			final InlineCompletion completion = InlineCompletionController.this.completion;
			if (completion != null) {
				final Point location = widget.getLocationAtOffset(completion.widgetOffset());
				final List<String> lines = completion.lines();
				event.gc.setBackground(new Color(200, 255, 200));
				event.gc.setForeground(widget.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
				event.gc.setFont(font);
				for (int i = 0; i < lines.size(); i++) {
					final String line = lines.get(i);
					if (i == 0) {
						// first line
						event.gc.drawText(completion.firstLineFillPrefix(), location.x, location.y, true);
						if (completion.firstLineSuffixCharacter() != null) {
							final int suffixCharacterWidth = event.gc.textExtent(completion.firstLineSuffixCharacter()).x;
							final int suffixWidth = event.gc.textExtent(completion.firstLineSuffix()).x;
							final int fillPrefixWidth = event.gc.textExtent(completion.firstLineFillPrefix()).x;
							final StyleRange styleRange = widget.getStyleRangeAtOffset(completion.widgetOffset());
							final int metricWidth = fillPrefixWidth + suffixCharacterWidth;
							if (needMetricUpdate(styleRange, metricWidth)) {
								updateMetrics(event, completion, widget, metricWidth);
							}
							event.gc.drawText(completion.firstLineSuffixCharacter(), location.x + fillPrefixWidth, location.y, false);
							event.gc.drawText(completion.firstLineFillSuffix(), location.x + fillPrefixWidth + suffixWidth, location.y, true);
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
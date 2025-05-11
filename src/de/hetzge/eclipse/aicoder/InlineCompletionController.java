package de.hetzge.eclipse.aicoder;

import java.io.IOException;
import java.util.List;
import java.util.Map;
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
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension2;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.StyledTextLineSpacingProvider;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.contexts.IContextActivation;
import org.eclipse.ui.texteditor.ITextEditor;

public final class InlineCompletionController {

	private static final Map<ITextViewer, InlineCompletionController> CONTROLLER_BY_VIEWER;
	static {
		CONTROLLER_BY_VIEWER = new ConcurrentHashMap<>();
	}

	public static InlineCompletionController setup(ITextEditor textEditor) {
		final ITextViewer textViewer = EditorUtils.getTextViewer(textEditor);
		return CONTROLLER_BY_VIEWER.computeIfAbsent(textViewer, ignore -> {
			final InlineCompletionController controller = new InlineCompletionController(textViewer, textEditor);
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

	private InlineCompletionController(ITextViewer textViewer, ITextEditor textEditor) {
		this.textViewer = textViewer;
		this.textEditor = textEditor;
		this.widget = textViewer.getTextWidget();
		this.spacingProvider = new StyledTextLineSpacingProviderImplementation();
		this.documentListener = new DocumentListenerImplementation();
		this.paintListener = new PaintListenerImplementation();
		this.painter = new PainterImplementation();
		this.keyListener = new KeyListenerImplementation();
		this.completion = null;
		this.context = null;
		this.job = null;

		final FontData[] fontData = this.widget.getFont().getFontData();
		for (int i = 0; i < fontData.length; ++i) {
			fontData[i].setStyle(fontData[i].getStyle() | SWT.ITALIC);
		}
		this.font = new Font(textViewer.getTextWidget().getDisplay(), fontData);
	}

	public void trigger() {
		try {
			final IDocument document = InlineCompletionController.this.textEditor.getDocumentProvider().getDocument(InlineCompletionController.this.textEditor.getEditorInput());
			final int offset = EditorUtils.getCurrentOffsetInDocument(InlineCompletionController.this.textEditor);
			final int line = document.getLineOfOffset(offset);
			final int firstLine = Math.max(0, line - 50);
			final int lastLine = Math.min(document.getNumberOfLines() - 1, line + 50);
			final String prefix = document.get(document.getLineOffset(firstLine), offset - document.getLineOffset(firstLine));
			final String suffix = document.get(offset, document.getLineOffset(lastLine) - offset);
			final int lineHeight = InlineCompletionController.this.textViewer.getTextWidget().getLineHeight();
			final int defaultLineSpacing = InlineCompletionController.this.textViewer.getTextWidget().getLineSpacing();
			InlineCompletionController.this.abort();
			if (this.job != null) {
				this.job.cancel();
			}
			this.job = Job.create("AI inline completion", monitor -> {
				try {
					// TODO provide single (as completion?!) and multiline
					final String content = MistralUtils.execute("TODO", prefix, suffix);
					final int lineSpacing = (int) (defaultLineSpacing + (content.lines().count() - 1) * lineHeight);
					setup(new Completion(line, offset, content, lineSpacing, lineHeight));
				} catch (final IOException exception) {
					throw new CoreException(Status.error("Failed to compute inline completion", exception));
				}
			});
			this.job.schedule();

		} catch (final BadLocationException exception) {
			throw new RuntimeException(exception);
		}
	}

	private void setup(Completion completion) {
		this.context = EditorUtils.getContextService(this.textEditor).activateContext("de.hetzge.eclipse.codestral.inlineCompletionVisible");
		this.completion = completion;
	}

	public void abort() {
		if (this.context != null) {
			EditorUtils.getContextService(this.textEditor).deactivateContext(this.context);
			this.context = null;
		}
		this.completion = null;
	}

	public void accept() throws CoreException {
		if (this.completion == null) {
			return;
		}
		final IDocument document = InlineCompletionController.this.textEditor.getDocumentProvider().getDocument(InlineCompletionController.this.textEditor.getEditorInput());
		try {
			final Completion completion = this.completion;
			document.replace(this.completion.offset, 0, this.completion.content); // triggers document change -> triggers abort
			this.textViewer.setSelectedRange(completion.offset + completion.content.length(), 0);
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
		@Override
		public void keyPressed(KeyEvent event) {
		}

		@Override
		public void keyReleased(KeyEvent event) {
		}
	}

	private class StyledTextLineSpacingProviderImplementation implements StyledTextLineSpacingProvider {
		@Override
		public Integer getLineSpacing(int lineIndex) {
			final Completion completion = InlineCompletionController.this.completion;
			if (completion != null && completion.lineIndex == lineIndex) {
				return completion.lineSpacing;
			}
			return null;
		}
	}

	private class PaintListenerImplementation implements PaintListener {
		@Override
		public void paintControl(PaintEvent event) {
			final Completion completion = InlineCompletionController.this.completion;
			if (completion != null) {
				final List<String> lines = completion.content.lines().toList();
				event.gc.setForeground(InlineCompletionController.this.textViewer.getTextWidget().getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
				event.gc.setFont(InlineCompletionController.this.font);
				final Point location = InlineCompletionController.this.textViewer.getTextWidget().getLocationAtOffset(completion.offset);
				for (int i = 0; i < lines.size(); i++) {
					final String line = lines.get(i);
					if (i == 0) {
						event.gc.drawText(line, location.x, location.y, true);
					} else {
						event.gc.drawText(line.replace("\t", " ".repeat(InlineCompletionController.this.widget.getTabs())), 0, location.y + i * completion.lineHeight, true);
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

	private record Completion(
			int lineIndex,
			int offset,
			String content,
			int lineSpacing,
			int lineHeight) {
	}

}
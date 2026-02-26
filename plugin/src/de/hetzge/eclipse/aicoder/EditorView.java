package de.hetzge.eclipse.aicoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;

public record EditorView(String path, int lineIndex, List<String> lines) {
	public EditorView {
		Objects.requireNonNull(path);
		Objects.requireNonNull(lines);
	}

	public int getEndLineIndex() {
		return this.lineIndex + this.lines.size();
	}

	public static Optional<EditorView> createFromTextEditor(ITextViewer textViewer) {
		final IDocument document = textViewer.getDocument();
		final int startLine = textViewer.getTopIndex();
		final int endLine = textViewer.getBottomIndex();
		final List<String> lines = new ArrayList<String>();
		for (int i = startLine; i <= endLine && i < document.getNumberOfLines(); i++) {
			try {
				final int lineOffset = document.getLineOffset(i);
				final int lineLength = document.getLineLength(i);
				lines.add(document.get(lineOffset, lineLength));
			} catch (final BadLocationException exception) {
				throw new IllegalStateException(String.format("Failed to get line information from document. Line: %s, End line: %s, Document length: %s", i, endLine, document.getLength()), exception);
			}
		}
		final String pathString = getPathString(document);
		return Optional.of(new EditorView(pathString, startLine, lines));
	}

	public static String getPathString(final IDocument document) {
		String pathString = ITextFileBufferManager.DEFAULT.getTextFileBuffer(document).getLocation().toOSString();
		pathString = pathString.startsWith("/") ? pathString.substring(1) : pathString;
		return pathString;
	}
}
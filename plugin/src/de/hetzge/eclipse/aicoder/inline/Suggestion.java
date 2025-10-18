package de.hetzge.eclipse.aicoder.inline;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import de.hetzge.eclipse.aicoder.history.AiCoderHistoryEntry;

public record Suggestion(
		AiCoderHistoryEntry historyEntry,
		String content,
		int modelOffset,
		int originalLength,
		int widgetLastLine,
		int newLines,
		int oldLines,
		int additionalLines) {

	public void applyTo(final IDocument document) throws BadLocationException {
		final int offset = this.modelOffset();
		final int length = this.originalLength();
		document.replace(offset, length, this.content());
	}
}

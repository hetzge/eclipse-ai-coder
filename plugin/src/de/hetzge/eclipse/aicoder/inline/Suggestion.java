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
		int oldLines) {

	public Suggestion withOffset(int additionalCharCount, int additionalLineCount) {
		return new Suggestion(
				this.historyEntry,
				this.content,
				this.modelOffset + additionalCharCount,
				this.originalLength,
				this.widgetLastLine + additionalLineCount, // TODO this can be problematic
				this.newLines,
				this.oldLines);
	}

	public int getAdditionalCharCount() {
		return this.content.length() - this.originalLength;
	}

	public int getAdditionalLineCount() {
		return Math.max(this.newLines - this.oldLines, 0);
	}

	public void applyTo(final IDocument document) throws BadLocationException {
		final int offset = this.modelOffset();
		final int length = this.originalLength();
		document.replace(offset, length, this.content());
	}
}

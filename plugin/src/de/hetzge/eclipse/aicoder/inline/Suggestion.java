package de.hetzge.eclipse.aicoder.inline;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;

import de.hetzge.eclipse.aicoder.history.AiCoderHistoryEntry;
import de.hetzge.eclipse.aicoder.util.EclipseUtils;

public record Suggestion(
		AiCoderHistoryEntry historyEntry,
		String content,
		int modelOffset,
		int originalLength,
		int newLines,
		int oldLines) {

	public int widgetLastLine(ITextViewer textViewer) throws BadLocationException {
		return EclipseUtils.getWidgetLine(textViewer, this.modelOffset) + this.oldLines - 1;
	}

	public Suggestion withOffset(int additionalCharCount) {
		return new Suggestion(
				this.historyEntry,
				this.content,
				this.modelOffset + additionalCharCount,
				this.originalLength,
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

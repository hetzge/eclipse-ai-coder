package de.hetzge.eclipse.aicoder.inline;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

public record InlineCompletion(
		int widgetLineIndex,
		IRegion modelRegion,
		int widgetOffset,
		String firstLineContent,
		String content,
		/*
		 * if the completion is not the end of the line, this contains the next letter (otherwise null)
		 */
		String firstLineSuffixCharacter,
		int lineSpacing,
		int lineHeight) {

	public String toDebugString() {
		return """
				=========================
				lineIndex: %d
				modelRegion: %s
				widgetOffset: %d
				firstLineContent: "%s"
				content:
				---
				%s
				---
				firstLineSuffixCharacter: "%s"
				lineSpacing: %d
				lineHeight: %d
				=========================
				""".formatted(this.widgetLineIndex, this.modelRegion, this.widgetOffset, this.firstLineContent, this.content, this.firstLineSuffixCharacter, this.lineSpacing, this.lineHeight);
	}

	public static InlineCompletion create(IDocument document, int modelOffset, int widgetOffset, int widgetLine, String content, int lineHeight, int defaultLineSpacing) throws BadLocationException {
		final int line = document.getLineOfOffset(modelOffset);
		final int nextLine = line + 1;
		final int lineSuffixLength;
		if (nextLine < document.getNumberOfLines()) {
			lineSuffixLength = document.getLineOffset(nextLine) - modelOffset - document.getLineDelimiter(line).length();
		} else {
			lineSuffixLength = document.getLength() - modelOffset;
		}
		final String lineSuffix = document.get(modelOffset, lineSuffixLength);
		final String firstLineSuffixCharacter = !lineSuffix.isBlank() ? lineSuffix.substring(0, 1) : null;
		String firstLineContent = content.lines().findFirst().orElse("");
		final boolean contentContainsLineSuffix = content.startsWith(lineSuffix);
		if (contentContainsLineSuffix) {
			content = content.substring(lineSuffixLength);
			firstLineContent = firstLineContent.substring(Math.min(lineSuffixLength, firstLineContent.length()));
		}
		final int lineSpacing = (int) (defaultLineSpacing + (content.lines().count() - 1) * lineHeight);
		final Region modelRegion = new Region(modelOffset, contentContainsLineSuffix ? lineSuffix.length() : 0);
		return new InlineCompletion(widgetLine, modelRegion, widgetOffset, firstLineContent, content, firstLineSuffixCharacter, lineSpacing, lineHeight);
	}
}

package de.hetzge.eclipse.aicoder.inline;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.history.AiCoderHistoryEntry;

public record InlineCompletion(
		AiCoderHistoryEntry historyEntry,
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

	public void applyTo(final IDocument document) throws BadLocationException {
		final int replaceOffset = this.modelRegion().getOffset();
		final int replaceLength = this.modelRegion().getLength();
		document.replace(replaceOffset, replaceLength, this.content());
	}

	public static InlineCompletion create(AiCoderHistoryEntry historyEntry, IDocument document, int modelOffset, int widgetOffset, int widgetLine, String content, int lineHeight, int defaultLineSpacing) throws BadLocationException {
		final boolean isMultiline = content.lines().count() > 1;
		// TODO validate if this is working
		if (isMultiline) {
			final String suffix = document.get(modelOffset, document.getLength() - modelOffset);
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
		final int line = document.getLineOfOffset(modelOffset);
		final int nextLine = line + 1;
		final int lineSuffixLength;
		if (nextLine < document.getNumberOfLines()) {
			lineSuffixLength = document.getLineOffset(nextLine) - modelOffset - document.getLineDelimiter(line).length();
		} else {
			lineSuffixLength = document.getLength() - modelOffset;
		}
		final String lineSuffix = document.get(modelOffset, lineSuffixLength);
		final String firstLineSuffixCharacter = !lineSuffix.isBlank() && !isMultiline ? lineSuffix.substring(0, 1) : null;
		String firstLineContent = content.lines().findFirst().orElse("");
		final boolean contentContainsLineSuffix = content.startsWith(lineSuffix);
		if (contentContainsLineSuffix) {
			content = content.substring(lineSuffixLength);
			firstLineContent = firstLineContent.substring(Math.min(lineSuffixLength, firstLineContent.length()));
		}
		final int lineSpacing = (int) (defaultLineSpacing + (content.lines().count() - 1) * lineHeight);
		final Region modelRegion = new Region(modelOffset, contentContainsLineSuffix || isMultiline ? lineSuffix.length() : 0);
		if (isMultiline && !lineSuffix.isBlank()) {
			content += lineSuffix;
		}
		return new InlineCompletion(historyEntry, widgetLine, modelRegion, widgetOffset, firstLineContent, content, firstLineSuffixCharacter, lineSpacing, lineHeight);
	}
}

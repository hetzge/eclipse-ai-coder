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
		String content, // the whole completion content
		List<String> lines, // the completion content line by line
		String firstLineFillPrefix,
		String firstLineFillSuffix,
		String firstLineSuffix, // the existing suffix of the line that is being completed
		String firstLineSuffixCharacter, // if the completion is not the end of the line, this contains the next letter (otherwise null)
		int lineSpacing,
		int lineHeight) {

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
		final String firstLineSuffix = document.get(modelOffset, lineSuffixLength);
		final String firstLineSuffixCharacter = !firstLineSuffix.isBlank() ? firstLineSuffix.substring(0, 1) : null;
		final String firstLineContent = content.lines().findFirst().orElse("");
		final boolean contentContainsLineSuffix = content.startsWith(firstLineSuffix);
		final int lineSpacing = (int) (defaultLineSpacing + (content.lines().count() - 1) * lineHeight);
		final Region modelRegion = new Region(modelOffset, contentContainsLineSuffix || isMultiline ? firstLineSuffix.length() : 0);
		final String firstLineFillPrefix;
		final String firstLineFillSuffix;
		if (!firstLineContent.isBlank() && !firstLineSuffix.isBlank() && firstLineContent.contains(firstLineSuffix)) {
			firstLineFillPrefix = firstLineContent.substring(0, firstLineContent.indexOf(firstLineSuffix));
			firstLineFillSuffix = firstLineContent.substring(firstLineContent.indexOf(firstLineSuffix) + firstLineSuffix.length());
		} else {
			firstLineFillPrefix = firstLineContent;
			firstLineFillSuffix = "";
		}
		return new InlineCompletion(historyEntry, widgetLine, modelRegion, widgetOffset, content, content.lines().toList(), firstLineFillPrefix, firstLineFillSuffix, firstLineSuffix, firstLineSuffixCharacter, lineSpacing, lineHeight);
	}
}

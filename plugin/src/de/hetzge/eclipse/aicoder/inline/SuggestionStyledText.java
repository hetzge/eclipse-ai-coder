package de.hetzge.eclipse.aicoder.inline;

import java.util.List;
import java.util.stream.Collectors;

import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch;
import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch.Diff;
import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch.Operation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;

import de.hetzge.eclipse.aicoder.util.Utils;

public final class SuggestionStyledText {

	private SuggestionStyledText() {
	}

	public static StyledText create(Composite parent, StyledText originalStyledText, String content) {
		final DiffMatchPatch diffMatchPatch = new DiffMatchPatch();
//		diffMatchPatch.matchDistance = 1000;
//		diffMatchPatch.diffEditCost = 1;
//		diffMatchPatch.matchThreshold = 0.5f;
//		diffMatchPatch.patchDeleteThreshold = 0.5f;
		final List<Diff> diffs = diffMatchPatch.diffMain(originalStyledText.getSelectionText(), Utils.stripCodeMarkdownTags(content));
		final StyledText suggestionStyledText = new StyledText(parent, SWT.BORDER);
		suggestionStyledText.setEditable(false);
		suggestionStyledText.setTabs(originalStyledText.getTabs());
		suggestionStyledText.setTabStops(originalStyledText.getTabStops());
		suggestionStyledText.setFont(originalStyledText.getFont());
		suggestionStyledText.setForeground(originalStyledText.getForeground());
		suggestionStyledText.setBackground(originalStyledText.getBackground());
		suggestionStyledText.setLineSpacing(originalStyledText.getLineSpacing());
		suggestionStyledText.setText(diffs.stream().map(it -> it.text).collect(Collectors.joining("")));
		int originalOffset = originalStyledText.getSelectionRange().x;
		int suggestionOffset = 0;
		for (final Diff diff : diffs) {
			if (diff.operation == Operation.DELETE) {
				int i = 0;
				while (i < diff.text.length()) {
					final StyleRange originalStyleRange = originalStyledText.getStyleRangeAtOffset(originalOffset + i);
					if (originalStyleRange == null) {
						i++;
						continue;
					}
					final StyleRange suggestionStyleRange = createCopy(originalStyleRange);
					suggestionStyleRange.background = new Color(255, 200, 200);
					suggestionStyleRange.start = suggestionOffset + i;
					suggestionStyleRange.length = Math.min(suggestionOffset + diff.text.length() - suggestionStyleRange.start, originalStyleRange.length);
					suggestionStyledText.setStyleRange(suggestionStyleRange);
					i = (originalStyleRange.start + originalStyleRange.length) - originalOffset;
				}
				originalOffset += diff.text.length();
				suggestionOffset += diff.text.length();
			} else if (diff.operation == Operation.INSERT) {
				final StyleRange suggestionStyleRange = new StyleRange();
				suggestionStyleRange.background = new Color(200, 255, 200);
				suggestionStyleRange.start = suggestionOffset;
				suggestionStyleRange.length = diff.text.length();
				suggestionStyledText.setStyleRange(suggestionStyleRange);
				suggestionOffset += diff.text.length();
			} else if (diff.operation == Operation.EQUAL) {
				int i = 0;
				while (i < diff.text.length()) {
					final StyleRange originalStyleRange = originalStyledText.getStyleRangeAtOffset(originalOffset + i);
					if (originalStyleRange == null) {
						i++;
						continue;
					}
					final StyleRange suggestionStyleRange = createCopy(originalStyleRange);
					suggestionStyleRange.start = suggestionOffset + i;
					suggestionStyleRange.length = Math.min(suggestionOffset + diff.text.length() - suggestionStyleRange.start, originalStyleRange.length);
					suggestionStyledText.setStyleRange(suggestionStyleRange);
					i = (originalStyleRange.start + originalStyleRange.length) - originalOffset;
				}
				originalOffset += diff.text.length();
				suggestionOffset += diff.text.length();
			}
		}
		return suggestionStyledText;
	}

	private static StyleRange createCopy(final StyleRange styleRange) {
		final StyleRange newStyleRange = new StyleRange();
		newStyleRange.background = styleRange.background;
		newStyleRange.borderColor = styleRange.borderColor;
		newStyleRange.borderStyle = styleRange.borderStyle;
		newStyleRange.font = styleRange.font;
		newStyleRange.fontStyle = styleRange.fontStyle;
		newStyleRange.foreground = styleRange.foreground;
		newStyleRange.metrics = styleRange.metrics;
		newStyleRange.rise = styleRange.rise;
		newStyleRange.strikeout = styleRange.strikeout;
		newStyleRange.strikeoutColor = styleRange.strikeoutColor;
		newStyleRange.underline = styleRange.underline;
		newStyleRange.underlineColor = styleRange.underlineColor;
		newStyleRange.underlineStyle = styleRange.underlineStyle;
		return newStyleRange;
	}
}

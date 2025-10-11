package de.hetzge.eclipse.aicoder.inline;

import java.util.List;
import java.util.stream.Collectors;

import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch;
import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch.Diff;
import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch.Operation;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;

import de.hetzge.eclipse.aicoder.util.EclipseUtils;

public final class SuggestionStyledText {

	private SuggestionStyledText() {
	}

	public static StyledText create(Composite parent, ITextViewer parentTextViewer, String newContent) {
		final StyledText parentStyledText = parentTextViewer.getTextWidget();
		final String originalContent = EclipseUtils.getSelectionText(parentTextViewer);
		final List<Diff> diffs = new DiffMatchPatch().diffMain(originalContent, newContent);
		final StyledText suggestionStyledText = new StyledText(parent, SWT.BORDER);
		suggestionStyledText.setEditable(false);
		suggestionStyledText.setTabs(parentStyledText.getTabs());
		suggestionStyledText.setTabStops(parentStyledText.getTabStops());
		suggestionStyledText.setFont(parentStyledText.getFont());
		suggestionStyledText.setForeground(parentStyledText.getForeground());
		suggestionStyledText.setBackground(parentStyledText.getBackground());
		suggestionStyledText.setLineSpacing(parentStyledText.getLineSpacing());
		suggestionStyledText.setText(diffs.stream().map(it -> it.text).collect(Collectors.joining()));
		int originalOffset = parentStyledText.getSelectionRange().x; // TODO handle collapsed
		int suggestionOffset = 0;
		for (final Diff diff : diffs) {
			if (diff.operation == Operation.DELETE) {
				int i = 0;
				while (i < diff.text.length()) {
					final StyleRange originalStyleRange = parentStyledText.getStyleRangeAtOffset(originalOffset + i);
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
					final StyleRange originalStyleRange = parentStyledText.getStyleRangeAtOffset(originalOffset + i);
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

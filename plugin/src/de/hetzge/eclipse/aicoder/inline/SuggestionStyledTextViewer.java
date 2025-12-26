package de.hetzge.eclipse.aicoder.inline;

import java.util.List;
import java.util.stream.Collectors;

import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch;
import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch.Diff;
import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch.Operation;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import de.hetzge.eclipse.aicoder.util.DiffUtils;
import de.hetzge.eclipse.aicoder.util.EclipseUtils;

public final class SuggestionStyledTextViewer {

	private final StyledText parentStyledText;
	private final String originalContent;
	private final StyledText styledText;
	private final String newContent;
	private final Color addedColor;
	private final Color removedColor;

	public SuggestionStyledTextViewer(Composite parent, ITextViewer parentTextViewer, String newContent) {
		this.parentStyledText = parentTextViewer.getTextWidget();
		if (isDarkColor(parentTextViewer.getTextWidget().getForeground())) {
			this.addedColor = new Color(200, 255, 200);
			this.removedColor = new Color(255, 200, 200);
		} else {
			this.addedColor = new Color(50, 100, 50);
			this.removedColor = new Color(100, 50, 50);
		}
		this.originalContent = EclipseUtils.getSelectionText(parentTextViewer);
		this.styledText = new StyledText(parent, SWT.BORDER);
		this.styledText.setEditable(false);
		this.styledText.setTabs(this.parentStyledText.getTabs());
		this.styledText.setTabStops(this.parentStyledText.getTabStops());
		this.styledText.setFont(this.parentStyledText.getFont());
		this.styledText.setForeground(this.parentStyledText.getForeground());
		this.styledText.setBackground(this.parentStyledText.getBackground());
		this.styledText.setLineSpacing(this.parentStyledText.getLineSpacing());
		this.styledText.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
		this.newContent = newContent;
	}

	public int getLineCount() {
		return this.styledText.getLineCount();
	}

	public void setupLineDiff() {
		this.styledText.setText("");
		final List<String> diffLines = DiffUtils.diff(this.originalContent, this.newContent).lines().toList();
		int originalOffset = this.parentStyledText.getSelectionRange().x;
		for (int i = 0; i < diffLines.size(); i++) {
			final String diffLine = diffLines.get(i);
			final String line = diffLine.substring(1);
			this.styledText.append(line);
			if (diffLine.startsWith("+")) {
				final StyleRange styleRange = new StyleRange();
				styleRange.start = this.styledText.getCharCount() - line.length();
				styleRange.length = line.length();
				styleRange.background = this.addedColor;
				this.styledText.setStyleRange(styleRange);
			} else if (diffLine.startsWith("-")) {
				final StyleRange styleRange = new StyleRange();
				styleRange.start = this.styledText.getCharCount() - line.length();
				styleRange.length = line.length();
				styleRange.background = this.removedColor;
				this.styledText.setStyleRange(styleRange);
				originalOffset += line.length() + 1;
			} else {
				int originalI = originalOffset;
				int lineI = 0;
				while (lineI < line.length()) {
					originalI = originalOffset + lineI;
					final StyleRange originalStyleRange = this.parentStyledText.getStyleRangeAtOffset(originalI);
					if (originalStyleRange == null) {
						lineI += 1;
						continue;
					}
					final StyleRange suggestionStyleRange = createCopy(originalStyleRange);
					suggestionStyleRange.start = this.styledText.getCharCount() - line.length() + lineI;
					suggestionStyleRange.length = Math.min(line.length() - lineI, originalStyleRange.length);
					this.styledText.setStyleRange(suggestionStyleRange);
					lineI += originalStyleRange.length;
				}
				originalOffset += line.length() + 1;
			}
			if (i < diffLines.size() - 1) {
				this.styledText.append("\n");
			}
		}
	}

	public void setupCharDiff() {
		this.styledText.setText("");
		final List<Diff> diffs = new DiffMatchPatch().diffMain(this.originalContent, this.newContent);
		this.styledText.setText(diffs.stream().map(it -> it.text).collect(Collectors.joining()));
		int originalOffset = this.parentStyledText.getSelectionRange().x; // TODO handle collapsed
		int suggestionOffset = 0;
		for (final Diff diff : diffs) {
			if (diff.operation == Operation.DELETE) {
				int i = 0;
				while (i < diff.text.length()) {
					final StyleRange originalStyleRange = this.parentStyledText.getStyleRangeAtOffset(originalOffset + i);
					if (originalStyleRange == null) {
						i++;
						continue;
					}
					final StyleRange suggestionStyleRange = createCopy(originalStyleRange);
					suggestionStyleRange.background = this.removedColor;
					suggestionStyleRange.start = suggestionOffset + i;
					suggestionStyleRange.length = Math.min(suggestionOffset + diff.text.length() - suggestionStyleRange.start, originalStyleRange.length);
					this.styledText.setStyleRange(suggestionStyleRange);
					i = (originalStyleRange.start + originalStyleRange.length) - originalOffset;
				}
				originalOffset += diff.text.length();
				suggestionOffset += diff.text.length();
			} else if (diff.operation == Operation.INSERT) {
				final StyleRange suggestionStyleRange = new StyleRange();
				suggestionStyleRange.background = this.addedColor;
				suggestionStyleRange.start = suggestionOffset;
				suggestionStyleRange.length = diff.text.length();
				this.styledText.setStyleRange(suggestionStyleRange);
				suggestionOffset += diff.text.length();
			} else if (diff.operation == Operation.EQUAL) {
				int i = 0;
				while (i < diff.text.length()) {
					final StyleRange originalStyleRange = this.parentStyledText.getStyleRangeAtOffset(originalOffset + i);
					if (originalStyleRange == null) {
						i++;
						continue;
					}
					final StyleRange suggestionStyleRange = createCopy(originalStyleRange);
					suggestionStyleRange.start = suggestionOffset + i;
					suggestionStyleRange.length = Math.min(suggestionOffset + diff.text.length() - suggestionStyleRange.start, originalStyleRange.length);
					this.styledText.setStyleRange(suggestionStyleRange);
					i = (originalStyleRange.start + originalStyleRange.length) - originalOffset;
				}
				originalOffset += diff.text.length();
				suggestionOffset += diff.text.length();
			}
		}
	}

	public void setupOriginalDiff() {
		this.styledText.setText("");
		this.styledText.setText(this.originalContent);
		final int originalOffset = this.parentStyledText.getSelectionRange().x; // TODO handle collapsed
		int i = 0;
		while (i < this.originalContent.length()) {
			final StyleRange originalStyleRange = this.parentStyledText.getStyleRangeAtOffset(originalOffset + i);
			if (originalStyleRange == null) {
				i++;
				continue;
			}
			final StyleRange suggestionStyleRange = createCopy(originalStyleRange);
			suggestionStyleRange.start = i;
			suggestionStyleRange.length = Math.min(this.originalContent.length() - suggestionStyleRange.start, originalStyleRange.length);
			this.styledText.setStyleRange(suggestionStyleRange);
			i = (originalStyleRange.start + originalStyleRange.length) - originalOffset;
		}
	}

	public void setupNewDiff() {
		this.styledText.setText("");
		this.styledText.setText(this.newContent);

		// add empty lines for each additional old line content
		final int additionalLines = (int) (this.originalContent.lines().count() - this.newContent.lines().count());
		for (int i = 0; i < additionalLines; i++) {
			this.styledText.append("\n");
		}
	}

	public Control getFocusControl() {
		return this.styledText;
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

	private static boolean isDarkColor(final Color color) {
		return color.getRed() + color.getGreen() + color.getBlue() < 382;
	}
}

package de.hetzge.eclipse.aicoder.inline;

import java.util.List;

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
		final StyledText suggestionStyledText = new StyledText(parent, SWT.BORDER);
		suggestionStyledText.setEditable(false);
		suggestionStyledText.setTabs(parentStyledText.getTabs());
		suggestionStyledText.setTabStops(parentStyledText.getTabStops());
		suggestionStyledText.setFont(parentStyledText.getFont());
		suggestionStyledText.setForeground(parentStyledText.getForeground());
		suggestionStyledText.setBackground(parentStyledText.getBackground());
		suggestionStyledText.setLineSpacing(parentStyledText.getLineSpacing());

		// ######### LINE DIFF ##########################

		final List<String> originalLines = originalContent.lines().toList();
		final List<String> newLines = newContent.lines().toList();
		final List<String> diffLines = diff(originalLines, newLines).lines().toList();

		int originalOffset = parentStyledText.getSelectionRange().x;

		for (int i = 0; i < diffLines.size(); i++) {
			final String diffLine = diffLines.get(i);
			final String line = diffLine.substring(1);
			suggestionStyledText.append(line);
			if (diffLine.startsWith("+")) {
				final StyleRange styleRange = new StyleRange();
				styleRange.start = suggestionStyledText.getCharCount() - line.length();
				styleRange.length = line.length();
				styleRange.background = new Color(200, 255, 200);
				suggestionStyledText.setStyleRange(styleRange);
			} else if (diffLine.startsWith("-")) {
				final StyleRange styleRange = new StyleRange();
				styleRange.start = suggestionStyledText.getCharCount() - line.length();
				styleRange.length = line.length();
				styleRange.background = new Color(255, 200, 200);
				suggestionStyledText.setStyleRange(styleRange);
				originalOffset += line.length() + 1;
			} else {
				int originalI = originalOffset;
				int lineI = 0;
				while (lineI < line.length()) {
					originalI = originalOffset + lineI;
					final StyleRange originalStyleRange = parentStyledText.getStyleRangeAtOffset(originalI);
					if (originalStyleRange == null) {
						lineI += 1;
						continue;
					}
					final StyleRange suggestionStyleRange = createCopy(originalStyleRange);
					suggestionStyleRange.start = suggestionStyledText.getCharCount() - line.length() + lineI;
					suggestionStyleRange.length = Math.min(line.length() - lineI, originalStyleRange.length);
					suggestionStyledText.setStyleRange(suggestionStyleRange);
					lineI += originalStyleRange.length;
				}
				originalOffset += line.length() + 1;
			}
			if (i < diffLines.size() - 1) {
				suggestionStyledText.append("\n");
			}
		}

		// ######### CHAR DIFF ##########################

//		final List<Diff> diffs = new DiffMatchPatch().diffMain(originalContent, newContent);
//		suggestionStyledText.setText(diffs.stream().map(it -> it.text).collect(Collectors.joining()));
//		int originalOffset = parentStyledText.getSelectionRange().x; // TODO handle collapsed
//		int suggestionOffset = 0;
//		for (final Diff diff : diffs) {
//			if (diff.operation == Operation.DELETE) {
//				int i = 0;
//				while (i < diff.text.length()) {
//					final StyleRange originalStyleRange = parentStyledText.getStyleRangeAtOffset(originalOffset + i);
//					if (originalStyleRange == null) {
//						i++;
//						continue;
//					}
//					final StyleRange suggestionStyleRange = createCopy(originalStyleRange);
//					suggestionStyleRange.background = new Color(255, 200, 200);
//					suggestionStyleRange.start = suggestionOffset + i;
//					suggestionStyleRange.length = Math.min(suggestionOffset + diff.text.length() - suggestionStyleRange.start, originalStyleRange.length);
//					suggestionStyledText.setStyleRange(suggestionStyleRange);
//					i = (originalStyleRange.start + originalStyleRange.length) - originalOffset;
//				}
//				originalOffset += diff.text.length();
//				suggestionOffset += diff.text.length();
//			} else if (diff.operation == Operation.INSERT) {
//				final StyleRange suggestionStyleRange = new StyleRange();
//				suggestionStyleRange.background = new Color(200, 255, 200);
//				suggestionStyleRange.start = suggestionOffset;
//				suggestionStyleRange.length = diff.text.length();
//				suggestionStyledText.setStyleRange(suggestionStyleRange);
//				suggestionOffset += diff.text.length();
//			} else if (diff.operation == Operation.EQUAL) {
//				int i = 0;
//				while (i < diff.text.length()) {
//					final StyleRange originalStyleRange = parentStyledText.getStyleRangeAtOffset(originalOffset + i);
//					if (originalStyleRange == null) {
//						i++;
//						continue;
//					}
//					final StyleRange suggestionStyleRange = createCopy(originalStyleRange);
//					suggestionStyleRange.start = suggestionOffset + i;
//					suggestionStyleRange.length = Math.min(suggestionOffset + diff.text.length() - suggestionStyleRange.start, originalStyleRange.length);
//					suggestionStyledText.setStyleRange(suggestionStyleRange);
//					i = (originalStyleRange.start + originalStyleRange.length) - originalOffset;
//				}
//				originalOffset += diff.text.length();
//				suggestionOffset += diff.text.length();
//			}
//		}
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

	public static void main(String[] args) {

		final String a = ""
				+ "one two three\n"
				+ "four five six\n"
				+ "seven eight nine\n";
		final String b = ""
				+ "one twu three\n"
				+ "four fife six\n"
				+ "seven eight nine\n";

		System.out.println(diff(a.lines().toList(), b.lines().toList()));

	}

	public static String diff(List<String> oldList, List<String> newList) {
		final int m = oldList.size();
		final int n = newList.size();

		// Build LCS matrix (O(m*n))
		final int[][] lcs = new int[m + 1][n + 1];
		for (int i = 0; i < m; i++) {
			for (int j = 0; j < n; j++) {
				lcs[i + 1][j + 1] = oldList.get(i).equals(newList.get(j)) ? lcs[i][j] + 1 : Math.max(lcs[i][j + 1], lcs[i + 1][j]);
			}
		}

		// Backtrack to produce diff hunks
		final StringBuilder sb = new StringBuilder();
		int i = m, j = n;
		while (i > 0 || j > 0) {
			if (i > 0 && j > 0 && oldList.get(i - 1).equals(newList.get(j - 1))) {
				sb.insert(0, " " + oldList.get(i - 1) + "\n");
				i--;
				j--;
			} else if (j > 0 && (i == 0 || lcs[i][j - 1] >= lcs[i - 1][j])) {
				sb.insert(0, "+" + newList.get(j - 1) + "\n");
				j--;
			} else if (i > 0 && (j == 0 || lcs[i][j - 1] < lcs[i - 1][j])) {
				sb.insert(0, "-" + oldList.get(i - 1) + "\n");
				i--;
			}
		}

		return sb.toString();
	}
}

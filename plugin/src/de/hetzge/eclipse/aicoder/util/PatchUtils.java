package de.hetzge.eclipse.aicoder.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.diff.HistogramDiff;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;

public final class PatchUtils {

	private static final Pattern HUNK_HEADER_PATTERN = Pattern.compile("@@ -(\\d+)(?:,(\\d+))? \\+(\\d+)(?:,(\\d+))? @@.*");
	private static final String NO_NEWLINE_MARKER = "\\ No newline at end of file";
	private static final int SEARCH_WINDOW = 200;
	private static final int EXACT_SCORE = 10;
	private static final int TRIMMED_SCORE = 5;
	private static final int MISMATCH_PENALTY = 10;
	private static final int EXACT_POSITION_BONUS = 5;

	private PatchUtils() {
	}

	public static String createPatch(String oldContent, String newContent) throws IOException {
		return createPatch(oldContent, newContent, "file.txt", "file.txt");
	}

	public static String createPatch(String oldContent, String newContent, String oldPath, String newPath) throws IOException {
		return createPatch(oldContent, newContent, oldPath, newPath, 5);
	}

	public static String createPatch(String oldContent, String newContent, String oldPath, String newPath, int context) throws IOException {
		if (oldContent == null) {
			oldContent = "";
		}
		if (newContent == null) {
			newContent = "";
		}
		if (oldPath == null || oldPath.isBlank()) {
			oldPath = "file.txt";
		}
		if (newPath == null || newPath.isBlank()) {
			newPath = "file.txt";
		}
		oldPath = oldPath.replace("\r", "").replace("\n", "");
		newPath = newPath.replace("\r", "").replace("\n", "");
		if (context < 0) {
			throw new IllegalArgumentException("context must be >= 0 but was " + context);
		}
		final RawText oldText = new RawText(oldContent.getBytes(StandardCharsets.UTF_8));
		final RawText newText = new RawText(newContent.getBytes(StandardCharsets.UTF_8));
		final EditList diffList = new EditList();
		diffList.addAll(new HistogramDiff().diff(RawTextComparator.DEFAULT, oldText, newText));
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (DiffFormatter formatter = new DiffFormatter(out)) {
			formatter.setContext(context);
			formatter.format(diffList, oldText, newText);
		}
		final String patchBody = out.toString(StandardCharsets.UTF_8);
		return "--- " + oldPath + "\n"
				+ "+++ " + newPath + "\n"
				+ patchBody;
	}

	/**
	 * Applies a unified diff patch to a string with tolerance for line number mismatches and surrounding code changes.
	 *
	 * @param original The original text
	 * @param patch    The unified diff patch
	 * @return The patched text, or null if patch cannot be applied
	 */
	public static String applyPatch(String original, String patch) {
		if (original == null || patch == null || patch.isEmpty()) {
			return original;
		}
		final String normalizedPatch = patch.replace("\r\n", "\n").replace("\r", "\n");
		final List<PatchHunk> hunks = parsePatch(normalizedPatch);
		if (hunks.isEmpty()) {
			return original;
		}

		final String lineSeparator = original.contains("\r\n") ? "\r\n" : "\n";
		final boolean originalEndsWithNewline = original.endsWith("\n") || original.endsWith("\r");
		final List<String> lines = splitLines(original);

		final List<String> currentLines = new ArrayList<>(lines);
		int offset = 0;
		for (final PatchHunk hunk : hunks) {
			final int adjustedExpected = clamp(hunk.oldStart + offset, 0, currentLines.size());
			final int matchAt = findBestMatch(currentLines, hunk, adjustedExpected);
			if (matchAt < 0) {
				return null;
			}
			final List<String> patched = applyHunkAt(currentLines, hunk, matchAt);
			if (patched == null) {
				return null;
			}
			offset += patched.size() - currentLines.size();
			currentLines.clear();
			currentLines.addAll(patched);
		}

		final boolean resultEndsWithNewline = decideTrailingNewline(originalEndsWithNewline, normalizedPatch, hunks, lines.size());
		final String joined = String.join("\n", currentLines);
		String result = joined.replace("\n", lineSeparator);
		if (!currentLines.isEmpty() && resultEndsWithNewline) {
			result += lineSeparator;
		}
		return result;
	}

	private static List<String> splitLines(String content) {
		if (content == null || content.isEmpty()) {
			return new ArrayList<>();
		}
		final String normalized = content.replace("\r\n", "\n").replace("\r", "\n");
		final boolean endsWithNewline = normalized.endsWith("\n");
		final String[] parts = normalized.split("\n", -1);
		final List<String> lines = new ArrayList<>(parts.length);
		Collections.addAll(lines, parts);
		if (endsWithNewline && !lines.isEmpty() && lines.get(lines.size() - 1).isEmpty()) {
			lines.remove(lines.size() - 1);
		}
		return lines;
	}

	private static boolean decideTrailingNewline(boolean originalEndsWithNewline, String normalizedPatch, List<PatchHunk> hunks, int originalLineCount) {
		if (hunks.isEmpty()) {
			return originalEndsWithNewline;
		}
		boolean touchesEnd = false;
		for (final PatchHunk hunk : hunks) {
			if (hunk.oldStart + hunk.oldCount >= originalLineCount) {
				touchesEnd = true;
				break;
			}
		}
		if (!touchesEnd) {
			return originalEndsWithNewline;
		}
		// The new content trailing newline state is encoded by the marker at the end of the patch.
		if (normalizedPatch.stripTrailing().endsWith(NO_NEWLINE_MARKER)) {
			return false;
		}
		if (normalizedPatch.contains(NO_NEWLINE_MARKER)) {
			return true;
		}
		return true;
	}

	private static int clamp(int value, int min, int max) {
		if (value < min) {
			return min;
		}
		return Math.min(value, max);
	}

	private static int findBestMatch(List<String> lines, PatchHunk hunk, int expected) {
		if (hunk.matchableCount() == 0) {
			return clamp(expected, 0, lines.size());
		}
		final int minimum = minimumScore(hunk);
		final int windowFrom = clamp(expected - SEARCH_WINDOW, 0, lines.size());
		final int windowTo = clamp(expected + SEARCH_WINDOW, 0, lines.size());
		final int windowBest = searchBest(lines, hunk, windowFrom, windowTo, expected);
		if (windowBest >= 0 && score(lines, windowBest, hunk, expected) >= minimum) {
			return windowBest;
		}
		final int fullBest = searchBest(lines, hunk, 0, lines.size(), expected);
		if (fullBest >= 0 && score(lines, fullBest, hunk, expected) >= minimum) {
			return fullBest;
		}
		return -1;
	}

	private static int searchBest(List<String> lines, PatchHunk hunk, int from, int to, int expected) {
		int bestIndex = -1;
		int bestScore = Integer.MIN_VALUE;
		for (int i = from; i <= to; i++) {
			final int currentScore = score(lines, i, hunk, expected);
			if (currentScore > bestScore) {
				bestScore = currentScore;
				bestIndex = i;
			}
		}
		return bestIndex;
	}

	private static int score(List<String> lines, int startLine, PatchHunk hunk, int expected) {
		int result = 0;
		int index = startLine;
		for (final String line : hunk.lines) {
			final char prefix = line.charAt(0);
			if (prefix == '+') {
				continue;
			}
			if (index >= lines.size()) {
				return Integer.MIN_VALUE / 2;
			}
			final String wanted = line.length() > 1 ? line.substring(1) : "";
			final String actual = lines.get(index);
			if (actual.equals(wanted)) {
				result += EXACT_SCORE;
			} else if (actual.trim().equals(wanted.trim())) {
				result += TRIMMED_SCORE;
			} else {
				result -= MISMATCH_PENALTY;
			}
			index++;
		}
		if (startLine == expected) {
			result += EXACT_POSITION_BONUS;
		} else {
			result += Math.max(0, 4 - Math.abs(startLine - expected) / 20);
		}
		return result;
	}

	private static int minimumScore(PatchHunk hunk) {
		final int matchable = hunk.matchableCount();
		if (matchable == 0) {
			return 0;
		}
		if (matchable == 1) {
			return TRIMMED_SCORE;
		}
		return matchable * TRIMMED_SCORE;
	}

	private static List<String> applyHunkAt(List<String> lines, PatchHunk hunk, int startLine) {
		if (startLine < 0 || startLine > lines.size()) {
			return null;
		}
		final List<String> before = new ArrayList<>(lines.subList(0, startLine));
		final List<String> middle = new ArrayList<>();
		final List<String> after;
		int index = startLine;
		for (final String line : hunk.lines) {
			final char prefix = line.charAt(0);
			if (prefix == '-') {
				if (index >= lines.size()) {
					return null;
				}
				index++;
			} else if (prefix == '+') {
				middle.add(line.length() > 1 ? line.substring(1) : "");
			} else {
				if (index >= lines.size()) {
					return null;
				}
				middle.add(lines.get(index));
				index++;
			}
		}
		after = new ArrayList<>(lines.subList(index, lines.size()));
		final List<String> result = new ArrayList<>(before.size() + middle.size() + after.size());
		result.addAll(before);
		result.addAll(middle);
		result.addAll(after);
		return result;
	}

	private static List<PatchHunk> parsePatch(String patch) {
		final List<PatchHunk> hunks = new ArrayList<>();
		final String[] rawLines = patch.split("\n", -1);
		PatchHunk current = null;
		final boolean endsWithNewline = patch.endsWith("\n") || patch.endsWith("\r");
		for (int lineIndex = 0; lineIndex < rawLines.length; lineIndex++) {
			final String rawLine = rawLines[lineIndex];
			final boolean isLast = lineIndex == rawLines.length - 1;
			String line = rawLine.endsWith("\r") ? rawLine.substring(0, rawLine.length() - 1) : rawLine;
			if (isLast && line.isEmpty() && endsWithNewline) {
				// Trailing split artifact, not a hunk line.
				continue;
			}
			if (line.startsWith("@@ ")) {
				final PatchHunk parsed = parseHunkHeader(line);
				if (parsed != null) {
					if (current != null) {
						hunks.add(current);
					}
					current = parsed;
					continue;
				}
			}
			if (current == null) {
				continue;
			}
			if (line.startsWith(NO_NEWLINE_MARKER) || line.startsWith("\\ ")) {
				continue;
			}
			if (line.isEmpty()) {
				// Blank context line emitted without leading space.
				current.addLine(" ");
				continue;
			}
			final char first = line.charAt(0);
			if (first == ' ' || first == '-' || first == '+') {
				// Keep adding even if header counts are off (tolerates LLM-generated patches).
				current.addLine(line);
				continue;
			}
			if (line.startsWith("diff --git") || line.startsWith("diff ") || line.startsWith("Index:") || line.startsWith("index ")) {
				hunks.add(current);
				current = null;
				continue;
			}
			if ((line.startsWith("--- ") || line.startsWith("+++ ")) && current.isComplete()) {
				hunks.add(current);
				current = null;
			}
		}
		if (current != null) {
			hunks.add(current);
		}
		return hunks;
	}

	private static PatchHunk parseHunkHeader(String header) {
		final Matcher matcher = HUNK_HEADER_PATTERN.matcher(header);
		if (!matcher.matches()) {
			return null;
		}
		final int oldStart = Math.max(0, Integer.parseInt(matcher.group(1)) - 1);
		final int oldCount = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : 1;
		final int newStart = Math.max(0, Integer.parseInt(matcher.group(3)) - 1);
		final int newCount = matcher.group(4) != null ? Integer.parseInt(matcher.group(4)) : 1;
		return new PatchHunk(oldStart, oldCount, newStart, newCount);
	}

	private static final class PatchHunk {
		private final int oldStart;
		private final int oldCount;
		private final int newStart;
		private final int newCount;
		private final List<String> lines = new ArrayList<>();
		private int oldConsumed;
		private int newConsumed;

		private PatchHunk(int oldStart, int oldCount, int newStart, int newCount) {
			this.oldStart = oldStart;
			this.oldCount = oldCount;
			this.newStart = newStart;
			this.newCount = newCount;
		}

		private void addLine(String line) {
			this.lines.add(line);
			final char prefix = line.charAt(0);
			if (prefix == ' ') {
				this.oldConsumed++;
				this.newConsumed++;
			} else if (prefix == '-') {
				this.oldConsumed++;
			} else if (prefix == '+') {
				this.newConsumed++;
			}
		}

		private boolean isComplete() {
			return this.oldConsumed >= this.oldCount && this.newConsumed >= this.newCount;
		}

		private int matchableCount() {
			return this.oldCount;
		}
	}
}

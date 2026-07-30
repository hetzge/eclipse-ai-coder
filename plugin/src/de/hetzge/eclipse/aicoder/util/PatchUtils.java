package de.hetzge.eclipse.aicoder.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jgit.diff.DiffAlgorithm;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.diff.HistogramDiff;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;

public final class PatchUtils {

	private PatchUtils() {
	}

	public static String createPatch(String oldContent, String newContent, String oldPath, String newPath) throws IOException {
		final RawText oldText = new RawText(oldContent.getBytes(StandardCharsets.UTF_8));
		final RawText newText = new RawText(newContent.getBytes(StandardCharsets.UTF_8));
		final EditList diffList = new EditList();
		diffList.addAll(new HistogramDiff().diff(RawTextComparator.DEFAULT, oldText, newText));
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (DiffFormatter formatter = new DiffFormatter(out)) {
			formatter.setDiffAlgorithm(DiffAlgorithm.getAlgorithm(DiffAlgorithm.SupportedAlgorithm.MYERS));
			formatter.setDiffComparator(RawTextComparator.DEFAULT);
			formatter.setContext(5);
			formatter.format(diffList, oldText, newText);
		}
		final String patchBody = out.toString(StandardCharsets.UTF_8.name());
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

		// Normalize line endings
		final String normalizedOriginal = original.replace("\r\n", "\n").replace("\r", "\n");

		// Split into lines, preserving empty lines at the end
		final String[] originalLines = normalizedOriginal.split("\n", -1);

		// Remove trailing empty line if original didn't end with newline
		if (!normalizedOriginal.endsWith("\n") && originalLines.length > 0
				&& originalLines[originalLines.length - 1].isEmpty()) {
			final String[] trimmed = new String[originalLines.length - 1];
			System.arraycopy(originalLines, 0, trimmed, 0, trimmed.length);
			return applyPatchToLines(trimmed, patch);
		}

		return applyPatchToLines(originalLines, patch);
	}

	private static String applyPatchToLines(String[] originalLines, String patch) {
		final List<PatchHunk> hunks = parsePatch(patch);

		// Process hunks in reverse order to maintain line numbers
		for (int i = hunks.size() - 1; i >= 0; i--) {
			final PatchHunk hunk = hunks.get(i);
			final String[] result = applyHunk(originalLines, hunk);
			if (result == null) {
				return null; // Failed to apply hunk
			}
			originalLines = result;
		}

		return String.join("\n", originalLines);
	}

	/**
	 * Applies a single hunk to the original lines array and returns the new array
	 */
	private static String[] applyHunk(String[] originalLines, PatchHunk hunk) {
		// Try to find the best match for the hunk context
		int bestMatchStart = -1;
		int bestMatchScore = -1;

		// Search range: allow some flexibility around the expected line number
		final int searchStart = Math.max(0, hunk.expectedStartLine - 5);
		final int searchEnd = Math.min(originalLines.length - getHunkContextSize(hunk),
				originalLines.length);

		for (int i = searchStart; i <= searchEnd; i++) {
			final int score = calculateMatchScore(originalLines, i, hunk);
			if (score > bestMatchScore) {
				bestMatchScore = score;
				bestMatchStart = i;
			}
		}

		// If no good match found, try exact line number match
		if (bestMatchScore < getMinimumMatchScore(hunk)) {
			if (hunk.expectedStartLine >= 0 && hunk.expectedStartLine < originalLines.length) {
				final int exactScore = calculateMatchScore(originalLines, hunk.expectedStartLine, hunk);
				if (exactScore >= getMinimumMatchScore(hunk)) {
					bestMatchStart = hunk.expectedStartLine;
					bestMatchScore = exactScore;
				}
			}
		}

		if (bestMatchStart == -1 || bestMatchScore < getMinimumMatchScore(hunk)) {
			return null;
		}

		// Apply the changes and return new array
		return applyChanges(originalLines, bestMatchStart, hunk);
	}

	private static int getHunkContextSize(PatchHunk hunk) {
		int size = 0;
		for (final String line : hunk.lines) {
			if (line.startsWith(" ") || line.startsWith("-")) {
				size++;
			}
		}
		return size;
	}

	private static int calculateMatchScore(String[] originalLines, int startLine, PatchHunk hunk) {
		int score = 0;
		int originalIndex = startLine;

		for (final String line : hunk.lines) {
			if (originalIndex >= originalLines.length) {
				break;
			}

			if (line.startsWith(" ")) {
				// Context line
				final String contextContent = line.substring(1);
				if (originalLines[originalIndex].equals(contextContent)) {
					score += 10;
				} else if (originalLines[originalIndex].trim().equals(contextContent.trim())) {
					score += 5;
				}
				originalIndex++;
			} else if (line.startsWith("-")) {
				// Deletion - check if original matches
				final String deletionContent = line.substring(1);
				if (originalIndex < originalLines.length) {
					if (originalLines[originalIndex].equals(deletionContent)) {
						score += 10;
					} else if (originalLines[originalIndex].trim().equals(deletionContent.trim())) {
						score += 5;
					}
					originalIndex++;
				}
			} else if (line.startsWith("+")) {
				// Addition - don't advance original index
			}
		}

		// Bonus for matching at expected line number
		if (startLine == hunk.expectedStartLine) {
			score += 20;
		}

		return score;
	}

	private static int getMinimumMatchScore(PatchHunk hunk) {
		// Count actual context and deletion lines (lines that should match original)
		int matchableLines = 0;
		for (final String line : hunk.lines) {
			if (line.startsWith(" ") || line.startsWith("-")) {
				matchableLines++;
			}
		}
		// Require at least 70% of matchable lines to match exactly
		return (int) Math.ceil(matchableLines * 0.7) * 10;
	}

	/**
	 * Applies the actual changes (additions/deletions) at the matched position and returns the new array
	 */
	private static String[] applyChanges(String[] originalLines, int startLine, PatchHunk hunk) {
		final List<String> result = new ArrayList<>();
		int originalIndex = startLine;

		for (final String line : hunk.lines) {
			if (line.startsWith("-")) {
				// Deletion - skip this line in original
				if (originalIndex < originalLines.length) {
					originalIndex++;
				}
			} else if (line.startsWith("+")) {
				// Addition - add new line
				result.add(line.substring(1));
			} else if (line.startsWith(" ")) {
				// Context line - keep original
				if (originalIndex < originalLines.length) {
					result.add(originalLines[originalIndex]);
					originalIndex++;
				}
			}
		}

		// Build the final array: before + result + after
		final List<String> finalLines = new ArrayList<>();

		// Add lines before the hunk
		for (int i = 0; i < startLine; i++) {
			finalLines.add(originalLines[i]);
		}

		// Add the result lines
		finalLines.addAll(result);

		// Add lines after the hunk
		for (int i = originalIndex; i < originalLines.length; i++) {
			finalLines.add(originalLines[i]);
		}

		return finalLines.toArray(new String[0]);
	}

	/**
	 * Parses a unified diff into hunks
	 */
	private static List<PatchHunk> parsePatch(String patch) {
		final List<PatchHunk> hunks = new ArrayList<>();
		final String[] lines = patch.split("\n");

		PatchHunk currentHunk = null;

		for (final String line : lines) {
			if (line.startsWith("--- ") || line.startsWith("+++ ")) {
				// Skip file headers
				continue;
			} else if (line.startsWith("@@ ")) {
				// New hunk
				if (currentHunk != null) {
					hunks.add(currentHunk);
				}
				currentHunk = parseHunkHeader(line);
			} else if (currentHunk != null &&
					(line.startsWith(" ") || line.startsWith("-") || line.startsWith("+"))) {
				currentHunk.lines.add(line);
				if (line.startsWith(" ")) {
					currentHunk.contextLines.add(line.substring(1));
				}
			}
		}

		if (currentHunk != null) {
			hunks.add(currentHunk);
		}

		return hunks;
	}

	/**
	 * Parses a hunk header like "@@ -1,5 +1,6 @@"
	 */
	private static PatchHunk parseHunkHeader(String header) {
		final PatchHunk hunk = new PatchHunk();

		// Extract line numbers from header
		final Pattern pattern = Pattern.compile("@@ -(\\d+)(?:,(\\d+))? \\+(\\d+)(?:,(\\d+))? @@");
		final Matcher matcher = pattern.matcher(header);

		if (matcher.matches()) {
			hunk.expectedStartLine = Integer.parseInt(matcher.group(1)) - 1; // Convert to 0-based
		} else {
			hunk.expectedStartLine = 0; // Default if parsing fails
		}

		return hunk;
	}

	private static class PatchHunk {
		int expectedStartLine;
		List<String> lines = new ArrayList<>();
		List<String> contextLines = new ArrayList<>();
	}
}
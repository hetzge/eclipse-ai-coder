package de.hetzge.eclipse.aicoder;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

public final class Utils {
	private Utils() {
	}

	/**
	 * Provides an approximate count of AI tokens in the given string.
	 *
	 * @param text The input text to estimate token count for
	 * @return An approximate number of tokens
	 */
	public static int countApproximateTokens(String text) {
		if (text == null || text.isEmpty()) {
			return 0;
		}

		// Split by whitespace for words
		final String[] words = text.split("\\s+");
		final int wordCount = words.length;

		// Count punctuation and special characters as they often become separate tokens
		int punctCount = 0;
		for (final char c : text.toCharArray()) {
			if (!Character.isLetterOrDigit(c) && !Character.isWhitespace(c)) {
				punctCount++;
			}
		}

		// Count numbers as they often become separate tokens
		int numberCount = 0;
		for (final String word : words) {
			if (word.matches("\\d+")) {
				numberCount++;
			}
		}

		// Base formula: approximately 4/3 words for English text
		// Plus adjustments for punctuation and numbers
		final int approximateTokens = (int) (wordCount * 1.3) + (punctCount / 2) + (numberCount / 3);

		// For very short texts, ensure we return at least 1 token if there's any
		// content
		return Math.max(1, approximateTokens);
	}

	public static String getTypeKeywordLabel(IType type) {
		final int flags = getFlags(type);
		if (Flags.isInterface(flags)) {
			return "interface";
		} else if (Flags.isEnum(flags)) {
			return "enum";
		} else if (Flags.isAnnotation(flags)) {
			return "annotation";
		} else if (Flags.isRecord(flags)) {
			return "record";
		} else {
			return "class";
		}
	}

	private static int getFlags(IType type) {
		try {
			return type.getFlags();
		} catch (final JavaModelException exception) {
			throw new RuntimeException("Failed to get type flags for " + type, exception);
		}
	}

	public static boolean checkType(IType type) {
		if (type == null) {
			return false;
		}
		try {
			type.getFlags();
			return true;
		} catch (final JavaModelException exception) {
			return false;
		}
	}

}

package de.hetzge.eclipse.aicoder.quicksearch;

import java.util.regex.Pattern;

/** Utility for compiling file-name regular expressions and common glob patterns. */
public final class FilePatternUtils {

	private FilePatternUtils() {
	}

	/**
	 * Compiles a file pattern. Patterns containing glob wildcards are accepted in
	 * addition to regular expressions, so values such as {@code *.java} work as
	 * expected and do not cause a dangling regular-expression quantifier.
	 */
	public static Pattern compile(String pattern) {
		if (isGlobPattern(pattern)) {
			return Pattern.compile(globToRegex(pattern));
		}
		return Pattern.compile(pattern);
	}

	private static boolean isGlobPattern(String pattern) {
		for (int i = 0; i < pattern.length(); i++) {
			final char character = pattern.charAt(i);
			if (character == '?' || (character == '*' && (i == 0 || pattern.charAt(i - 1) != '.'))) {
				return true;
			}
		}
		return false;
	}

	private static String globToRegex(String glob) {
		final StringBuilder regex = new StringBuilder("^");
		for (int i = 0; i < glob.length(); i++) {
			final char character = glob.charAt(i);
			switch (character) {
			case '*':
				regex.append(".*");
				break;
			case '?':
				regex.append('.');
				break;
			case '\\':
				regex.append("\\\\");
				break;
			default:
				if (".^$+{}[]()|".indexOf(character) >= 0) {
					regex.append('\\');
				}
				regex.append(character);
			}
		}
		return regex.append('$').toString();
	}
}

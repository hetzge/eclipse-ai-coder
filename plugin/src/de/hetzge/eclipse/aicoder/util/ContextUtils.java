package de.hetzge.eclipse.aicoder.util;

public final class ContextUtils {

	private ContextUtils() {
	}

	public static String contentTemplate(String title, String content) {
		if (content == null || content.isBlank()) {
			return "";
		}
		return String.format("# %s\n%s\n", title, content);
	}

	public static String codeTemplate(String title, String code) {
		if (code == null || code.isBlank()) {
			return "";
		}
		return String.format("## %s\n````\n%s\n````\n", title, code);
	}

	public static String listEntryTemplate(String text) {
		if (text == null || text.isBlank()) {
			return "";
		}
		return String.format("- %s\n", text);
	}

}

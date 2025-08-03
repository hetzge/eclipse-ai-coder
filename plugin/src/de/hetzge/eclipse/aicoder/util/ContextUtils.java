package de.hetzge.eclipse.aicoder.util;

public final class ContextUtils {

	private ContextUtils() {
	}

	public static String contentTemplate(String title, String content) {
		return String.format("# %s\n%s\n", title, content);
	}

	public static String codeTemplate(String title, String code) {
		return String.format("## %s\n````\n%s\n````\n", title, code);
	}

	public static String listEntryTemplate(String text) {
		return String.format("- %s\n", text);
	}

}

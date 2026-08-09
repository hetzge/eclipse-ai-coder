package de.hetzge.eclipse.aicoder;

import org.eclipse.jface.preference.IPreferenceStore;

import de.hetzge.eclipse.aicoder.preferences.AiCoderPreferences;
import de.hetzge.eclipse.aicoder.util.EclipseUtils;

/**
 * Stores the scratchpad content and the enabled flag.
 */
public final class ScratchpadStorage {

	private ScratchpadStorage() {
	}

	private static IPreferenceStore getStore() {
		return AiCoderActivator.getDefault().getPreferenceStore();
	}

	public static String getContent() {
		return getStore().getString(AiCoderPreferences.SCRATCHPAD_CONTENT_KEY);
	}

	public static void setContent(String content) {
		getStore().setValue(AiCoderPreferences.SCRATCHPAD_CONTENT_KEY, content == null ? "" : content);
	}

	public static boolean isEnabled() {
		return getStore().getBoolean(AiCoderPreferences.SCRATCHPAD_ENABLED_KEY);
	}

	public static void setEnabled(boolean enabled) {
		getStore().setValue(AiCoderPreferences.SCRATCHPAD_ENABLED_KEY, enabled);
	}

	public static void append(String text) {
		if (text == null || text.isEmpty()) {
			return;
		}
		final String currentContent = getContent();
		final StringBuilder builder = new StringBuilder(currentContent);
		if (!currentContent.isEmpty() && !currentContent.endsWith("\n")) {
			builder.append('\n');
		}
		builder.append(text);
		if (!text.endsWith("\n")) {
			builder.append('\n');
		}
		setContent(builder.toString());
		ScratchpadView.refreshContentIfOpen();
	}
}

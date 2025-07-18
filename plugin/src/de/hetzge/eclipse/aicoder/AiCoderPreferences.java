package de.hetzge.eclipse.aicoder;

import java.time.Duration;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

public class AiCoderPreferences extends AbstractPreferenceInitializer {

	// Preference keys
	public static final String AI_PROVIDER_PREFERENCE_KEY = "de.hetzge.eclipse.aicoder.ai_provider";

	public static final String CODESTRAL_API_KEY_PREFERENCE_KEY = "de.hetzge.eclipse.aicoder.codestral_api_key";
	public static final String OLLAMA_BASE_URL_PREFERENCE_KEY = "de.hetzge.eclipse.aicoder.ollama_base_url";
	public static final String OLLAMA_MODEL_PREFERENCE_KEY = "de.hetzge.eclipse.aicoder.ollama_model";
	public static final String ENABLE_MULTILINE_PREFERENCE_KEY = "de.hetzge.eclipse.aicoder.enable_multiline";
	public static final String ENABLE_AUTOCOMPLETE_KEY = "de.hetzge.eclipse.aicoder.enable_autocomplete";
	public static final String ONLY_ON_CHANGE_AUTOCOMPLETE_PREFERENCE_KEY = "de.hetzge.eclipse.aicoder.only_on_change_autocomplete";
	public static final String MAX_PREFIX_SIZE_KEY = "de.hetzge.eclipse.aicoder.max_prefix_size";
	public static final String MAX_SUFFIX_SIZE_KEY = "de.hetzge.eclipse.aicoder.max_suffix_size";
	public static final String MAX_TOKENS_KEY = "de.hetzge.eclipse.aicoder.max_tokens";
	public static final String IGNORE_JRE_CLASSES_KEY = "de.hetzge.eclipse.aicoder.ignore_jre_classes";
	public static final String DEBOUNCE_IN_MS_KEY = "de.hetzge.eclipse.aicoder.debounce_in_ms";

	@Override
	public void initializeDefaultPreferences() {
		final IPreferenceStore store = AiCoderActivator.getDefault().getPreferenceStore();
		store.setDefault(AI_PROVIDER_PREFERENCE_KEY, AiProvider.MISTRAL.name());

		store.setDefault(CODESTRAL_API_KEY_PREFERENCE_KEY, "");
		store.setDefault(OLLAMA_BASE_URL_PREFERENCE_KEY, "http://localhost:11434");
		store.setDefault(OLLAMA_MODEL_PREFERENCE_KEY, "qwen2.5-coder:3b");
		store.setDefault(ENABLE_MULTILINE_PREFERENCE_KEY, true);
		store.setDefault(ENABLE_AUTOCOMPLETE_KEY, true);
		store.setDefault(ONLY_ON_CHANGE_AUTOCOMPLETE_PREFERENCE_KEY, true);
		store.setDefault(MAX_PREFIX_SIZE_KEY, 1000);
		store.setDefault(MAX_SUFFIX_SIZE_KEY, 1000);
		store.setDefault(MAX_TOKENS_KEY, 1024);
		store.setDefault(IGNORE_JRE_CLASSES_KEY, true);
		store.setDefault(DEBOUNCE_IN_MS_KEY, 400);
	}

	public static AiProvider getAiProvider() {
		final String providerId = AiCoderActivator.getDefault().getPreferenceStore().getString(AI_PROVIDER_PREFERENCE_KEY);
		return AiProvider.valueOf(providerId.toUpperCase());
	}



	public static String getCodestralApiKey() {
		return AiCoderActivator.getDefault().getPreferenceStore().getString(CODESTRAL_API_KEY_PREFERENCE_KEY);
	}

	public static String getOllamaBaseUrl() {
		return AiCoderActivator.getDefault().getPreferenceStore().getString(OLLAMA_BASE_URL_PREFERENCE_KEY);
	}

	public static String getOllamaModel() {
		return AiCoderActivator.getDefault().getPreferenceStore().getString(OLLAMA_MODEL_PREFERENCE_KEY);
	}

	public static boolean isMultilineEnabled() {
		return AiCoderActivator.getDefault().getPreferenceStore().getBoolean(ENABLE_MULTILINE_PREFERENCE_KEY);
	}

	public static void setMultilineEnabled(boolean enabled) {
		AiCoderActivator.getDefault().getPreferenceStore().setValue(ENABLE_MULTILINE_PREFERENCE_KEY, enabled);
	}

	public static boolean isAutocompleteEnabled() {
		return AiCoderActivator.getDefault().getPreferenceStore().getBoolean(ENABLE_AUTOCOMPLETE_KEY);
	}

	public static boolean isOnlyOnChangeAutocompleteEnabled() {
		return AiCoderActivator.getDefault().getPreferenceStore().getBoolean(ONLY_ON_CHANGE_AUTOCOMPLETE_PREFERENCE_KEY);
	}

	public static int getMaxPrefixSize() {
		return AiCoderActivator.getDefault().getPreferenceStore().getInt(MAX_PREFIX_SIZE_KEY);
	}

	public static int getMaxSuffixSize() {
		return AiCoderActivator.getDefault().getPreferenceStore().getInt(MAX_SUFFIX_SIZE_KEY);
	}

	public static int getMaxTokens() {
		return AiCoderActivator.getDefault().getPreferenceStore().getInt(MAX_TOKENS_KEY);
	}

	public static boolean isIgnoreJreClasses() {
		return AiCoderActivator.getDefault().getPreferenceStore().getBoolean(IGNORE_JRE_CLASSES_KEY);
	}

	public static Duration getDebounceDuration() {
		return Duration.ofMillis(AiCoderActivator.getDefault().getPreferenceStore().getInt(DEBOUNCE_IN_MS_KEY));
	}
}
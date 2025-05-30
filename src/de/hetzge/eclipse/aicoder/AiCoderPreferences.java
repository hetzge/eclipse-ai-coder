package de.hetzge.eclipse.aicoder;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

public class AiCoderPreferences extends AbstractPreferenceInitializer {

    // Preference keys
    public static final String AI_PROVIDER_PREFERENCE_KEY = "de.hetzge.eclipse.aicoder.ai_provider";
    public static final String DEVSTRAL_BASE_URL_PREFERENCE_KEY = "de.hetzge.eclipse.aicoder.devstral_base_url";
    public static final String DEVSTRAL_API_KEY_PREFERENCE_KEY = "de.hetzge.eclipse.aicoder.devstral_api_key";
    public static final String ENABLE_MULTILINE_PREFERENCE_KEY = "de.hetzge.eclipse.aicoder.enable_multiline";

    // Default values
    private static final String DEFAULT_AI_PROVIDER = "mistral";
    private static final String DEFAULT_DEVSTRAL_BASE_URL = "https://codestral.mistral.ai/v1/fim/completions";
    private static final String DEFAULT_DEVSTRAL_API_KEY = "";
    private static final boolean DEFAULT_ENABLE_MULTILINE = true;

    @Override
    public void initializeDefaultPreferences() {
        final IPreferenceStore store = AiCoderActivator.getDefault().getPreferenceStore();
        store.setDefault(AI_PROVIDER_PREFERENCE_KEY, DEFAULT_AI_PROVIDER);
        store.setDefault(DEVSTRAL_BASE_URL_PREFERENCE_KEY, DEFAULT_DEVSTRAL_BASE_URL);
        store.setDefault(DEVSTRAL_API_KEY_PREFERENCE_KEY, DEFAULT_DEVSTRAL_API_KEY);
        store.setDefault(ENABLE_MULTILINE_PREFERENCE_KEY, DEFAULT_ENABLE_MULTILINE);
    }

    public static String getAiProvider() {
        return AiCoderActivator.getDefault().getPreferenceStore().getString(AI_PROVIDER_PREFERENCE_KEY);
    }

    public static String getDevstralBaseUrl() {
        return AiCoderActivator.getDefault().getPreferenceStore().getString(DEVSTRAL_BASE_URL_PREFERENCE_KEY);
    }

    public static String getDevstralApiKey() {
        return AiCoderActivator.getDefault().getPreferenceStore().getString(DEVSTRAL_API_KEY_PREFERENCE_KEY);
    }

    public static boolean isMultilineEnabled() {
        return AiCoderActivator.getDefault().getPreferenceStore().getBoolean(ENABLE_MULTILINE_PREFERENCE_KEY);
    }
}
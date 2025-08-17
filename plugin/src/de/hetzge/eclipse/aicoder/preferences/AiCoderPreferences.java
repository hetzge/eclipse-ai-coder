package de.hetzge.eclipse.aicoder.preferences;

import java.time.Duration;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.llm.LlmOption;
import de.hetzge.eclipse.aicoder.llm.LlmProvider;
import mjson.Json;

public final class AiCoderPreferences extends AbstractPreferenceInitializer {

	public static final String CODESTRAL_API_KEY_KEY = "de.hetzge.eclipse.aicoder.codestral_api_key";
	public static final String OLLAMA_BASE_URL_KEY = "de.hetzge.eclipse.aicoder.ollama_base_url";
	public static final String OPENAI_BASE_URL_KEY = "de.hetzge.eclipse.aicoder.openai_base_url";
	public static final String OPENAI_API_KEY_KEY = "de.hetzge.eclipse.aicoder.openai_api_key";
	public static final String FILL_IN_MIDDLE_PROVIDER_KEY = "de.hetzge.eclipse.aicoder.fill_in_middle_provider";
	public static final String FILL_IN_MIDDLE_MODEL_KEY = "de.hetzge.eclipse.aicoder.fill_in_middle_model";
	public static final String EDIT_PROVIDER_KEY = "de.hetzge.eclipse.aicoder.edit_provider";
	public static final String EDIT_MODEL_KEY = "de.hetzge.eclipse.aicoder.edit_model";
	public static final String ENABLE_MULTILINE_KEY = "de.hetzge.eclipse.aicoder.enable_multiline";
	public static final String ENABLE_AUTOCOMPLETE_KEY = "de.hetzge.eclipse.aicoder.enable_autocomplete";
	public static final String ONLY_ON_CHANGE_AUTOCOMPLETE_KEY = "de.hetzge.eclipse.aicoder.only_on_change_autocomplete";
	public static final String MAX_PREFIX_SIZE_KEY = "de.hetzge.eclipse.aicoder.max_prefix_size";
	public static final String MAX_SUFFIX_SIZE_KEY = "de.hetzge.eclipse.aicoder.max_suffix_size";
	public static final String MAX_TOKENS_KEY = "de.hetzge.eclipse.aicoder.max_tokens";
	public static final String IGNORE_JRE_CLASSES_KEY = "de.hetzge.eclipse.aicoder.ignore_jre_classes";
	public static final String DEBOUNCE_IN_MS_KEY = "de.hetzge.eclipse.aicoder.debounce_in_ms";
	public static final String MCP_SERVER_CONFIGURATIONS_KEY = "de.hetzge.eclipse.aicoder.mcp.server_configurations";
	public static final String CLEANUP_CODE_ON_APPLY_KEY = "de.hetzge.eclipse.aicoder.cleanup_code_on_apply";

	@Override
	public void initializeDefaultPreferences() {
		final IPreferenceStore store = getStore();
		store.setDefault(CODESTRAL_API_KEY_KEY, "");
		store.setDefault(OLLAMA_BASE_URL_KEY, "http://localhost:11434");
		store.setDefault(OPENAI_BASE_URL_KEY, "https://api.openai.com");
		store.setDefault(OPENAI_API_KEY_KEY, "");
		store.setDefault(FILL_IN_MIDDLE_PROVIDER_KEY, LlmProvider.MISTRAL.name());
		store.setDefault(FILL_IN_MIDDLE_MODEL_KEY, "codestral-latest");
		store.setDefault(EDIT_PROVIDER_KEY, LlmProvider.MISTRAL.name());
		store.setDefault(EDIT_MODEL_KEY, "codestral-latest");
		store.setDefault(ENABLE_MULTILINE_KEY, true);
		store.setDefault(ENABLE_AUTOCOMPLETE_KEY, true);
		store.setDefault(ONLY_ON_CHANGE_AUTOCOMPLETE_KEY, true);
		store.setDefault(MAX_PREFIX_SIZE_KEY, 1000);
		store.setDefault(MAX_SUFFIX_SIZE_KEY, 1000);
		store.setDefault(MAX_TOKENS_KEY, 1024);
		store.setDefault(IGNORE_JRE_CLASSES_KEY, true);
		store.setDefault(DEBOUNCE_IN_MS_KEY, 400);
		store.setDefault(MCP_SERVER_CONFIGURATIONS_KEY, "{}");
		store.setDefault(CLEANUP_CODE_ON_APPLY_KEY, true);
	}

	public static String getCodestralApiKey() {
		return getStore().getString(CODESTRAL_API_KEY_KEY);
	}

	public static String getOllamaBaseUrl() {
		return getStore().getString(OLLAMA_BASE_URL_KEY);
	}

	public static String getOpenAiBaseUrl() {
		return getStore().getString(OPENAI_BASE_URL_KEY);
	}

	public static String getOpenAiApiKey() {
		return getStore().getString(OPENAI_API_KEY_KEY);
	}

	public static LlmProvider getFillInMiddleProvider() {
		return LlmProvider.valueOf(getStore().getString(FILL_IN_MIDDLE_PROVIDER_KEY));
	}

	public static String getFillInMiddleModel() {
		return getStore().getString(FILL_IN_MIDDLE_MODEL_KEY);
	}

	public static LlmProvider getEditProvider() {
		return LlmProvider.valueOf(getStore().getString(EDIT_PROVIDER_KEY));
	}

	public static String getEditModel() {
		return getStore().getString(EDIT_MODEL_KEY);
	}

	public static void setEditLlmModelOption(LlmOption llmModelOption) {
		getStore().setValue(EDIT_PROVIDER_KEY, llmModelOption.provider().name());
		getStore().setValue(EDIT_MODEL_KEY, llmModelOption.modelKey());
	}

	public static boolean isMultilineEnabled() {
		return getStore().getBoolean(ENABLE_MULTILINE_KEY);
	}

	public static void setMultilineEnabled(boolean enabled) {
		getStore().setValue(ENABLE_MULTILINE_KEY, enabled);
	}

	public static boolean isAutocompleteEnabled() {
		return getStore().getBoolean(ENABLE_AUTOCOMPLETE_KEY);
	}

	public static boolean isOnlyOnChangeAutocompleteEnabled() {
		return getStore().getBoolean(ONLY_ON_CHANGE_AUTOCOMPLETE_KEY);
	}

	public static int getMaxPrefixSize() {
		return getStore().getInt(MAX_PREFIX_SIZE_KEY);
	}

	public static int getMaxSuffixSize() {
		return getStore().getInt(MAX_SUFFIX_SIZE_KEY);
	}

	public static int getMaxTokens() {
		return getStore().getInt(MAX_TOKENS_KEY);
	}

	public static boolean isIgnoreJreClasses() {
		return getStore().getBoolean(IGNORE_JRE_CLASSES_KEY);
	}

	public static Duration getDebounceDuration() {
		return Duration.ofMillis(getStore().getInt(DEBOUNCE_IN_MS_KEY));
	}

	public static Json getMcpServerConfigurations() {
		return Json.read(getStore().getString(MCP_SERVER_CONFIGURATIONS_KEY));
	}

	public static Json getDefaultMcpServerConfigurations() {
		return Json.read(getStore().getDefaultString(MCP_SERVER_CONFIGURATIONS_KEY));
	}

	public static void setMcpServerConfigurations(Json json) {
		getStore().setValue(MCP_SERVER_CONFIGURATIONS_KEY, json.toString());
	}

	private static IPreferenceStore getStore() {
		return AiCoderActivator.getDefault().getPreferenceStore();
	}

	public static boolean isCleanupCodeOnApplyEnabled() {
		return getStore().getBoolean(CLEANUP_CODE_ON_APPLY_KEY);
	}
}
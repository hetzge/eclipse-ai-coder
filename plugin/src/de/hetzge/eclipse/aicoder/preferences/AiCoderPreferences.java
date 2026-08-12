package de.hetzge.eclipse.aicoder.preferences;

import java.time.Duration;
import java.util.List;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.CompletionMode;
import de.hetzge.eclipse.aicoder.inline.DiffMode;
import de.hetzge.eclipse.aicoder.llm.LlmOption;
import de.hetzge.eclipse.aicoder.llm.LlmPromptTemplates;
import de.hetzge.eclipse.aicoder.llm.LlmProvider;
import mjson.Json;

public final class AiCoderPreferences extends AbstractPreferenceInitializer {

	public static final String CODESTRAL_API_KEY_KEY = "de.hetzge.eclipse.aicoder.codestral_api_key";
	public static final String OLLAMA_BASE_URL_KEY = "de.hetzge.eclipse.aicoder.ollama_base_url";
	public static final String OPENAI_BASE_URL_KEY = "de.hetzge.eclipse.aicoder.openai_base_url";
	public static final String OPENAI_API_KEY_KEY = "de.hetzge.eclipse.aicoder.openai_api_key";
	public static final String OPENAI_2_BASE_URL_KEY = "de.hetzge.eclipse.aicoder.openai2_base_url";
	public static final String OPENAI_2_API_KEY_KEY = "de.hetzge.eclipse.aicoder.openai2_api_key";
	public static final String OPENAI_3_BASE_URL_KEY = "de.hetzge.eclipse.aicoder.openai3_base_url";
	public static final String OPENAI_3_API_KEY_KEY = "de.hetzge.eclipse.aicoder.openai3_api_key";
	public static final String FILL_IN_MIDDLE_PROVIDER_KEY = "de.hetzge.eclipse.aicoder.fill_in_middle_provider";
	public static final String FILL_IN_MIDDLE_MODEL_KEY = "de.hetzge.eclipse.aicoder.fill_in_middle_model";
	public static final String QUICK_FIX_PROVIDER_KEY = "de.hetzge.eclipse.aicoder.quick_fix_provider";
	public static final String QUICK_FIX_MODEL_KEY = "de.hetzge.eclipse.aicoder.quick_fix_model";
	public static final String QUICK_FIX_PROMPT_KEY = "de.hetzge.eclipse.aicoder.quick_fix_prompt";
	public static final String GENERATE_PROVIDER_KEY = "de.hetzge.eclipse.aicoder.generate_provider";
	public static final String GENERATE_MODEL_KEY = "de.hetzge.eclipse.aicoder.generate_model";
	public static final String EDIT_PROVIDER_KEY = "de.hetzge.eclipse.aicoder.edit_provider";
	public static final String EDIT_MODEL_KEY = "de.hetzge.eclipse.aicoder.edit_model";
	public static final String NEXT_EDIT_PROVIDER_KEY = "de.hetzge.eclipse.aicoder.next_edit_provider";
	public static final String NEXT_EDIT_MODEL_KEY = "de.hetzge.eclipse.aicoder.next_edit_model";
	public static final String QUERY_PROVIDER_KEY = "de.hetzge.eclipse.aicoder.query_provider";
	public static final String QUERY_MODEL_KEY = "de.hetzge.eclipse.aicoder.query_model";
	public static final String RERANK_PROVIDER_KEY = "de.hetzge.eclipse.aicoder.rerank_provider";
	public static final String RERANK_MODEL_KEY = "de.hetzge.eclipse.aicoder.rerank_model";
	public static final String AGENT_PROVIDER_KEY = "de.hetzge.eclipse.aicoder.agent_provider";
	public static final String AGENT_MODEL_KEY = "de.hetzge.eclipse.aicoder.agent_model";
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
	public static final String DIFF_MODE_KEY = "de.hetzge.eclipse.aicoder.diff_mode";
	public static final String CHANGE_CODE_SYSTEM_PROMPT_KEY = "de.hetzge.eclipse.aicoder.change_code_system_prompt";
	public static final String GENERATE_CODE_SYSTEM_PROMPT_KEY = "de.hetzge.eclipse.aicoder.generate_code_system_prompt";
	public static final String QUERY_SYSTEM_PROMPT_KEY = "de.hetzge.eclipse.aicoder.query_system_prompt";
	public static final String FIM_TEMPLATE_KEY = "de.hetzge.eclipse.aicoder.fim_template";
	public static final String ENABLE_PSEUDO_FIM_KEY = "de.hetzge.eclipse.aicoder.enable_pseduo_fim";
	public static final String PSEUDO_FIM_SYSTEM_PROMPT_KEY = "de.hetzge.eclipse.aicoder.pseudo_fim_system_prompt";
	public static final String TIMEOUT_KEY = "de.hetzge.eclipse.aicoder.timeout";
	public static final String INCEPTIONLABS_API_KEY_KEY = "de.hetzge.eclipse.aicoder.inceptionlabs_api_key";
	public static final String OPENROUTER_API_KEY_KEY = "de.hetzge.eclipse.aicoder.openrouter_api_key";
	public static final String OLLAMA_NUM_CTX_KEY = "de.hetzge.eclipse.aicoder.ollama_num_ctx";
	public static final String SEARCH_TOOL_RESULT_LIMIT_KEY = "de.hetzge.eclipse.aicoder.search_tool_result_limit";
	public static final String SEARCH_TOOL_LINE_CONTENT_LENGTH_KEY = "de.hetzge.eclipse.aicoder.search_tool_line_content_length";
	public static final String READ_FILE_DEFAULT_MAX_LINE_COUNT_KEY = "de.hetzge.eclipse.aicoder.read_file_default_max_line_count";
	public static final String MAX_AGENT_ITERATIONS_KEY = "de.hetzge.eclipse.aicoder.max_agent_iterations";
	public static final String TOOL_CALL_OUTPUT_LIMIT_KEY = "de.hetzge.eclipse.aicoder.tool_call_output_limit";
	public static final String SCRATCHPAD_CONTENT_KEY = "de.hetzge.eclipse.aicoder.scratchpad_content";
	public static final String SCRATCHPAD_ENABLED_KEY = "de.hetzge.eclipse.aicoder.scratchpad_enabled";
	public static final String CODE_VIEWPORT_MEMORY_MAX_LINES_KEY = "de.hetzge.eclipse.aicoder.code_viewport_memory_max_lines";
	public static final String FILE_TREE_WHITELIST_KEY = "de.hetzge.eclipse.aicoder.file_tree_whitelist";
	public static final String FILE_TREE_BLACKLIST_KEY = "de.hetzge.eclipse.aicoder.file_tree_blacklist";
	public static final String AI_RERANK_WHITELIST_KEY = "de.hetzge.eclipse.aicoder.ai_rerank_whitelist";
	public static final String AI_RERANK_BLACKLIST_KEY = "de.hetzge.eclipse.aicoder.ai_rerank_blacklist";

	@Override
	public void initializeDefaultPreferences() {
		final IPreferenceStore store = getStore();
		store.setDefault(CODESTRAL_API_KEY_KEY, IPreferenceStore.STRING_DEFAULT_DEFAULT);
		store.setDefault(OLLAMA_BASE_URL_KEY, "http://localhost:11434");
		store.setDefault(OPENAI_BASE_URL_KEY, "https://api.openai.com");
		store.setDefault(OPENAI_API_KEY_KEY, IPreferenceStore.STRING_DEFAULT_DEFAULT);
		store.setDefault(OPENAI_2_BASE_URL_KEY, "");
		store.setDefault(OPENAI_2_API_KEY_KEY, IPreferenceStore.STRING_DEFAULT_DEFAULT);
		store.setDefault(OPENAI_3_BASE_URL_KEY, "");
		store.setDefault(OPENAI_3_API_KEY_KEY, IPreferenceStore.STRING_DEFAULT_DEFAULT);
		store.setDefault(FILL_IN_MIDDLE_PROVIDER_KEY, LlmProvider.NONE.name());
		store.setDefault(FILL_IN_MIDDLE_MODEL_KEY, IPreferenceStore.STRING_DEFAULT_DEFAULT);
		store.setDefault(QUICK_FIX_PROVIDER_KEY, LlmProvider.NONE.name());
		store.setDefault(QUICK_FIX_MODEL_KEY, IPreferenceStore.STRING_DEFAULT_DEFAULT);
		store.setDefault(QUICK_FIX_PROMPT_KEY, "Fix/complete the code");
		store.setDefault(GENERATE_PROVIDER_KEY, LlmProvider.NONE.name());
		store.setDefault(GENERATE_MODEL_KEY, IPreferenceStore.STRING_DEFAULT_DEFAULT);
		store.setDefault(EDIT_PROVIDER_KEY, LlmProvider.NONE.name());
		store.setDefault(EDIT_MODEL_KEY, IPreferenceStore.STRING_DEFAULT_DEFAULT);
		store.setDefault(NEXT_EDIT_PROVIDER_KEY, LlmProvider.NONE.name());
		store.setDefault(NEXT_EDIT_MODEL_KEY, IPreferenceStore.STRING_DEFAULT_DEFAULT);
		store.setDefault(AGENT_PROVIDER_KEY, LlmProvider.NONE.name());
		store.setDefault(AGENT_MODEL_KEY, IPreferenceStore.STRING_DEFAULT_DEFAULT);
		store.setDefault(RERANK_PROVIDER_KEY, LlmProvider.NONE.name());
		store.setDefault(RERANK_MODEL_KEY, IPreferenceStore.STRING_DEFAULT_DEFAULT);
		store.setDefault(ENABLE_MULTILINE_KEY, true);
		store.setDefault(ENABLE_AUTOCOMPLETE_KEY, true);
		store.setDefault(ONLY_ON_CHANGE_AUTOCOMPLETE_KEY, true);
		store.setDefault(MAX_PREFIX_SIZE_KEY, 100);
		store.setDefault(MAX_SUFFIX_SIZE_KEY, 100);
		store.setDefault(MAX_TOKENS_KEY, 1024);
		store.setDefault(IGNORE_JRE_CLASSES_KEY, true);
		store.setDefault(DEBOUNCE_IN_MS_KEY, 400);
		store.setDefault(MCP_SERVER_CONFIGURATIONS_KEY, Json.object().toString());
		store.setDefault(CLEANUP_CODE_ON_APPLY_KEY, true);
		store.setDefault(DIFF_MODE_KEY, DiffMode.LINE.name());
		store.setDefault(CHANGE_CODE_SYSTEM_PROMPT_KEY, LlmPromptTemplates.changeCodeSystemPrompt());
		store.setDefault(GENERATE_CODE_SYSTEM_PROMPT_KEY, LlmPromptTemplates.generateCodeSystemPrompt());
		store.setDefault(FIM_TEMPLATE_KEY, "<|fim_prefix|>{{prefix}}<|fim_suffix|>{{suffix}}<|fim_middle|>");
		store.setDefault(ENABLE_PSEUDO_FIM_KEY, false);
		store.setDefault(PSEUDO_FIM_SYSTEM_PROMPT_KEY, LlmPromptTemplates.pseudoFimCodeSystemPrompt());
		store.setDefault(QUERY_SYSTEM_PROMPT_KEY, LlmPromptTemplates.querySystemPrompt());
		store.setDefault(TIMEOUT_KEY, Duration.ofMinutes(5).toMillis());
		store.setDefault(INCEPTIONLABS_API_KEY_KEY, IPreferenceStore.STRING_DEFAULT_DEFAULT);
		store.setDefault(OPENROUTER_API_KEY_KEY, IPreferenceStore.STRING_DEFAULT_DEFAULT);
		store.setDefault(OLLAMA_NUM_CTX_KEY, 128000);
		store.setDefault(SEARCH_TOOL_RESULT_LIMIT_KEY, 1000);
		store.setDefault(SEARCH_TOOL_LINE_CONTENT_LENGTH_KEY, 1000);
		store.setDefault(READ_FILE_DEFAULT_MAX_LINE_COUNT_KEY, 2000);
		store.setDefault(MAX_AGENT_ITERATIONS_KEY, 200);
		store.setDefault(TOOL_CALL_OUTPUT_LIMIT_KEY, 200000);
		store.setDefault(SCRATCHPAD_CONTENT_KEY, "");
		store.setDefault(SCRATCHPAD_ENABLED_KEY, true);
		store.setDefault(CODE_VIEWPORT_MEMORY_MAX_LINES_KEY, 1000);
		store.setDefault(FILE_TREE_WHITELIST_KEY, "");
		store.setDefault(FILE_TREE_BLACKLIST_KEY, "");
		store.setDefault(AI_RERANK_WHITELIST_KEY, "");
		store.setDefault(AI_RERANK_BLACKLIST_KEY, "");
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

	public static String getOpenAi2BaseUrl() {
		return getStore().getString(OPENAI_2_BASE_URL_KEY);
	}

	public static String getOpenAi2ApiKey() {
		return getStore().getString(OPENAI_2_API_KEY_KEY);
	}

	public static String getOpenAi3BaseUrl() {
		return getStore().getString(OPENAI_3_BASE_URL_KEY);
	}

	public static String getOpenAi3ApiKey() {
		return getStore().getString(OPENAI_3_API_KEY_KEY);
	}

	public static void setLlmModelOption(CompletionMode mode, LlmOption llmModelOption) {
		switch (mode) {
		case INLINE:
			setFillInMiddleLlmModelOption(llmModelOption);
			break;
		case EDIT:
			setEditLlmModelOption(llmModelOption);
			break;
		case QUICK_FIX:
			setQuickFixLlmModelOption(llmModelOption);
			break;
		case GENERATE:
			setGenerateLlmModelOption(llmModelOption);
			break;
		case NEXT_EDIT:
			setNextEditLlmModelOption(llmModelOption);
			break;
		case AGENT:
			setAgentLlmModelOption(llmModelOption);
			break;
		case QUERY:
			setQueryLlmModelOption(llmModelOption);
			break;
		default:
			throw new IllegalArgumentException("Unsupported completion mode: " + mode);
		}
	}

	public static LlmProvider getFillInMiddleProvider() {
		return LlmProvider.valueOf(getStore().getString(FILL_IN_MIDDLE_PROVIDER_KEY));
	}

	public static String getFillInMiddleModel() {
		return getStore().getString(FILL_IN_MIDDLE_MODEL_KEY);
	}

	public static void setFillInMiddleLlmModelOption(LlmOption llmModelOption) {
		getStore().setValue(FILL_IN_MIDDLE_PROVIDER_KEY, llmModelOption.provider().name());
		getStore().setValue(FILL_IN_MIDDLE_MODEL_KEY, llmModelOption.modelKey());
	}

	public static LlmProvider getQuickFixProvider() {
		return LlmProvider.valueOf(getStore().getString(QUICK_FIX_PROVIDER_KEY));
	}

	public static String getQuickFixModel() {
		return getStore().getString(QUICK_FIX_MODEL_KEY);
	}

	public static void setQuickFixLlmModelOption(LlmOption llmModelOption) {
		getStore().setValue(QUICK_FIX_PROVIDER_KEY, llmModelOption.provider().name());
		getStore().setValue(QUICK_FIX_MODEL_KEY, llmModelOption.modelKey());
	}

	public static LlmProvider getGenerateProvider() {
		return LlmProvider.valueOf(getStore().getString(GENERATE_PROVIDER_KEY));
	}

	public static String getGenerateModel() {
		return getStore().getString(GENERATE_MODEL_KEY);
	}

	public static void setGenerateLlmModelOption(LlmOption llmModelOption) {
		getStore().setValue(GENERATE_PROVIDER_KEY, llmModelOption.provider().name());
		getStore().setValue(GENERATE_MODEL_KEY, llmModelOption.modelKey());
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

	public static LlmProvider getNextEditProvider() {
		return LlmProvider.valueOf(getStore().getString(NEXT_EDIT_PROVIDER_KEY));
	}

	public static String getNextEditModel() {
		return getStore().getString(NEXT_EDIT_MODEL_KEY);
	}

	public static void setNextEditLlmModelOption(LlmOption llmModelOption) {
		getStore().setValue(NEXT_EDIT_PROVIDER_KEY, llmModelOption.provider().name());
		getStore().setValue(NEXT_EDIT_MODEL_KEY, llmModelOption.modelKey());
	}

	public static LlmProvider getAgentProvider() {
		return LlmProvider.valueOf(getStore().getString(AGENT_PROVIDER_KEY));
	}

	public static String getAgentModel() {
		return getStore().getString(AGENT_MODEL_KEY);
	}

	public static void setAgentLlmModelOption(LlmOption llmModelOption) {
		getStore().setValue(AGENT_PROVIDER_KEY, llmModelOption.provider().name());
		getStore().setValue(AGENT_MODEL_KEY, llmModelOption.modelKey());
	}

	public static LlmProvider getQueryProvider() {
		return LlmProvider.valueOf(getStore().getString(QUERY_PROVIDER_KEY));
	}

	public static String getQueryModel() {
		return getStore().getString(QUERY_MODEL_KEY);
	}

	public static void setQueryLlmModelOption(LlmOption llmModelOption) {
		getStore().setValue(QUERY_PROVIDER_KEY, llmModelOption.provider().name());
		getStore().setValue(QUERY_MODEL_KEY, llmModelOption.modelKey());
	}

	public static LlmProvider getRerankProvider() {
		return LlmProvider.valueOf(getStore().getString(RERANK_PROVIDER_KEY));
	}

	public static String getRerankModel() {
		return getStore().getString(RERANK_MODEL_KEY);
	}

	public static void setRerankLlmModelOption(LlmOption llmModelOption) {
		getStore().setValue(RERANK_PROVIDER_KEY, llmModelOption.provider().name());
		getStore().setValue(RERANK_MODEL_KEY, llmModelOption.modelKey());
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

	public static DiffMode getDiffMode() {
		return DiffMode.valueOf(getStore().getString(DIFF_MODE_KEY));
	}

	public static void setDiffMode(DiffMode diffMode) {
		getStore().setValue(DIFF_MODE_KEY, diffMode.name());
	}

	public static String getQuickFixPrompt() {
		return getStore().getString(QUICK_FIX_PROMPT_KEY);
	}

	public static String getChangeCodeSystemPrompt() {
		return getStore().getString(CHANGE_CODE_SYSTEM_PROMPT_KEY);
	}

	public static String getGenerateCodeSystemPrompt() {
		return getStore().getString(GENERATE_CODE_SYSTEM_PROMPT_KEY);
	}

	public static String getFimTemplate() {
		return getStore().getString(FIM_TEMPLATE_KEY);
	}

	public static boolean isEnablePseduoFim() {
		return getStore().getBoolean(ENABLE_PSEUDO_FIM_KEY);
	}

	public static String getPseudoFimSystemPrompt() {
		return getStore().getString(PSEUDO_FIM_SYSTEM_PROMPT_KEY);
	}

	public static String getQuerySystemPrompt() {
		return getStore().getString(QUERY_SYSTEM_PROMPT_KEY);
	}

	public static Duration getTimeout() {
		return Duration.ofSeconds(getStore().getInt(TIMEOUT_KEY));
	}

	public static String getInceptionLabsApiKey() {
		return getStore().getString(INCEPTIONLABS_API_KEY_KEY);
	}

	public static String getOpenRouterApiKey() {
		return getStore().getString(OPENROUTER_API_KEY_KEY);
	}

	public static int getOllamaNumCtx() {
		return getStore().getInt(OLLAMA_NUM_CTX_KEY);
	}

	public static int getSearchToolResultLimit() {
		return getStore().getInt(SEARCH_TOOL_RESULT_LIMIT_KEY);
	}

	public static int getSearchToolLineContentLength() {
		return getStore().getInt(SEARCH_TOOL_LINE_CONTENT_LENGTH_KEY);
	}

	public static int getReadFileDefaultMaxLineCount() {
		return getStore().getInt(READ_FILE_DEFAULT_MAX_LINE_COUNT_KEY);
	}

	public static int getMaxAgentIterations() {
		return getStore().getInt(MAX_AGENT_ITERATIONS_KEY);
	}

	public static int getToolCallOutputLimit() {
		return getStore().getInt(TOOL_CALL_OUTPUT_LIMIT_KEY);
	}

	public static int getCodeViewportMemoryMaxLines() {
		return getStore().getInt(CODE_VIEWPORT_MEMORY_MAX_LINES_KEY);
	}

	public static List<String> getFileTreeWhitelist() {
		return getListPreference(FILE_TREE_WHITELIST_KEY);
	}

	public static List<String> getFileTreeBlacklist() {
		return getListPreference(FILE_TREE_BLACKLIST_KEY);
	}

	public static List<String> getAiRerankWhitelist() {
		return getListPreference(AI_RERANK_WHITELIST_KEY);
	}

	public static List<String> getAiRerankBlacklist() {
		return getListPreference(AI_RERANK_BLACKLIST_KEY);
	}

	private static List<String> getListPreference(String key) {
		final String value = getStore().getString(key);
		if (value == null || value.isBlank()) {
			return List.of();
		}
		return java.util.Arrays.stream(value.split(","))
				.map(String::trim)
				.filter(entry -> !entry.isEmpty())
				.toList();
	}
}

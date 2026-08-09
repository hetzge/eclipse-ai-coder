package de.hetzge.eclipse.aicoder.llm;

import de.hetzge.eclipse.aicoder.CompletionMode;
import de.hetzge.eclipse.aicoder.preferences.AiCoderPreferences;
import mjson.Json;

public record LlmOption(
		LlmProvider provider,
		String modelKey) {

	public String getLabel() {
		return this.provider.name() + " - " + this.modelKey;
	}

	public Json toJson() {
		return Json.object()
				.set("provider", this.provider.name())
				.set("modelKey", this.modelKey);
	}

	public static LlmOption fromJson(Json json) {
		return new LlmOption(LlmProvider.valueOf(json.at("provider").asString()), json.at("modelKey").asString());
	}

	public static LlmOption createModelOptionFromPreferences(CompletionMode mode) {
		switch (mode) {
		case INLINE:
			return createFillInMiddleModelOptionFromPreferences();
		case EDIT:
			return createEditModelOptionFromPreferences();
		case QUICK_FIX:
			return createQuickFixModelOptionFromPreferences();
		case GENERATE:
			return createGenerateModelOptionFromPreferences();
		case NEXT_EDIT:
			return createNextEditModelOptionFromPreferences();
		case AGENT:
			return createAgentModelOptionFromPreferences();
		case QUERY:
			return createQueryModelOptionFromPreferences();
		default:
			throw new IllegalArgumentException("Unsupported completion mode: " + mode);
		}
	}

	public static LlmOption createFillInMiddleModelOptionFromPreferences() {
		return new LlmOption(AiCoderPreferences.getFillInMiddleProvider(), AiCoderPreferences.getFillInMiddleModel());
	}

	public static LlmOption createEditModelOptionFromPreferences() {
		return new LlmOption(AiCoderPreferences.getEditProvider(), AiCoderPreferences.getEditModel());
	}

	public static LlmOption createGenerateModelOptionFromPreferences() {
		return new LlmOption(AiCoderPreferences.getGenerateProvider(), AiCoderPreferences.getGenerateModel());
	}

	public static LlmOption createQuickFixModelOptionFromPreferences() {
		return new LlmOption(AiCoderPreferences.getQuickFixProvider(), AiCoderPreferences.getQuickFixModel());
	}

	public static LlmOption createNextEditModelOptionFromPreferences() {
		return new LlmOption(AiCoderPreferences.getNextEditProvider(), AiCoderPreferences.getNextEditModel());
	}

	public static LlmOption createRerankModelOptionFromPreferences() {
		return new LlmOption(AiCoderPreferences.getRerankProvider(), AiCoderPreferences.getRerankModel());
	}

	public static LlmOption createAgentModelOptionFromPreferences() {
		return new LlmOption(AiCoderPreferences.getAgentProvider(), AiCoderPreferences.getAgentModel());
	}

	public static LlmOption createQueryModelOptionFromPreferences() {
		return new LlmOption(AiCoderPreferences.getQueryProvider(), AiCoderPreferences.getQueryModel());
	}
}

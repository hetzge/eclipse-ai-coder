package de.hetzge.eclipse.aicoder.llm;

import de.hetzge.eclipse.aicoder.preferences.AiCoderPreferences;

public record LlmOption(
		LlmProvider provider,
		String modelKey) {

	public String getLabel() {
		return this.provider.name() + " - " + this.modelKey;
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
}

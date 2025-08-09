package de.hetzge.eclipse.aicoder.llm;

import de.hetzge.eclipse.aicoder.preferences.AiCoderPreferences;

public record LlmModelOption(
		LlmProvider provider,
		String modelKey) {

	public String getLabel() {
		return this.provider.name() + " - " + this.modelKey;
	}

	public static LlmModelOption createFillInMiddleModelOptionFromPreferences() {
		return new LlmModelOption(AiCoderPreferences.getFillInMiddleProvider(), AiCoderPreferences.getFillInMiddleModel());
	}

	public static LlmModelOption createEditModelOptionFromPreferences() {
		return new LlmModelOption(AiCoderPreferences.getEditProvider(), AiCoderPreferences.getEditModel());
	}
}

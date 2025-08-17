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
}

package de.hetzge.eclipse.aicoder.preferences;

public class OpenAIPreferencePage extends AbstractOpenAiPreferencePage {

	public OpenAIPreferencePage() {
		super("OpenAI provider settings", AiCoderPreferences.OPENAI_BASE_URL_KEY, AiCoderPreferences.OPENAI_API_KEY_KEY);
	}
}

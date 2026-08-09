package de.hetzge.eclipse.aicoder.preferences;

public class OpenAi3PreferencePage extends AbstractOpenAiPreferencePage {

	public OpenAi3PreferencePage() {
		super("OpenAI 3 provider settings", AiCoderPreferences.OPENAI_3_BASE_URL_KEY, AiCoderPreferences.OPENAI_3_API_KEY_KEY);
	}
}

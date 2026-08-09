package de.hetzge.eclipse.aicoder.preferences;

public class OpenAi2PreferencePage extends AbstractOpenAiPreferencePage {

	public OpenAi2PreferencePage() {
		super("OpenAI 2 provider settings", AiCoderPreferences.OPENAI_2_BASE_URL_KEY, AiCoderPreferences.OPENAI_2_API_KEY_KEY);
	}
}

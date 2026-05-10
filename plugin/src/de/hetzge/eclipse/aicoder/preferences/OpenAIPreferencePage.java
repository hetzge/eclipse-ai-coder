package de.hetzge.eclipse.aicoder.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import de.hetzge.eclipse.aicoder.AiCoderActivator;

public class OpenAIPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public OpenAIPreferencePage() {
		super(GRID);
		setPreferenceStore(AiCoderActivator.getDefault().getPreferenceStore());
		setDescription("OpenAI provider settings");
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	protected void createFieldEditors() {
		addField(new StringFieldEditor(
				AiCoderPreferences.OPENAI_BASE_URL_KEY,
				"Base url:",
				getFieldEditorParent()));
		final StringFieldEditor openAiApiKeyFieldEditor = new StringFieldEditor(
				AiCoderPreferences.OPENAI_API_KEY_KEY,
				"API key:",
				getFieldEditorParent());
		openAiApiKeyFieldEditor.getTextControl(getFieldEditorParent()).setEchoChar('*');
		addField(openAiApiKeyFieldEditor);
	}
}

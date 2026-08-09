package de.hetzge.eclipse.aicoder.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import de.hetzge.eclipse.aicoder.AiCoderActivator;

public class OpenAi3PreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public OpenAi3PreferencePage() {
		super(GRID);
		setPreferenceStore(AiCoderActivator.getDefault().getPreferenceStore());
		setDescription("OpenAI 3 provider settings");
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	protected void createFieldEditors() {
		addField(new StringFieldEditor(
				AiCoderPreferences.OPENAI_3_BASE_URL_KEY,
				"Base url:",
				getFieldEditorParent()));
		final StringFieldEditor openAi3ApiKeyFieldEditor = new StringFieldEditor(
				AiCoderPreferences.OPENAI_3_API_KEY_KEY,
				"API key:",
				getFieldEditorParent());
		openAi3ApiKeyFieldEditor.getTextControl(getFieldEditorParent()).setEchoChar('*');
		addField(openAi3ApiKeyFieldEditor);
	}
}

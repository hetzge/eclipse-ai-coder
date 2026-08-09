package de.hetzge.eclipse.aicoder.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import de.hetzge.eclipse.aicoder.AiCoderActivator;

public class OpenAi2PreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public OpenAi2PreferencePage() {
		super(GRID);
		setPreferenceStore(AiCoderActivator.getDefault().getPreferenceStore());
		setDescription("OpenAI 2 provider settings");
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	protected void createFieldEditors() {
		addField(new StringFieldEditor(
				AiCoderPreferences.OPENAI_2_BASE_URL_KEY,
				"Base url:",
				getFieldEditorParent()));
		final StringFieldEditor openAi2ApiKeyFieldEditor = new StringFieldEditor(
				AiCoderPreferences.OPENAI_2_API_KEY_KEY,
				"API key:",
				getFieldEditorParent());
		openAi2ApiKeyFieldEditor.getTextControl(getFieldEditorParent()).setEchoChar('*');
		addField(openAi2ApiKeyFieldEditor);
	}
}

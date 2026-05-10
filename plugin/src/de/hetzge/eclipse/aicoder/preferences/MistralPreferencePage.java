package de.hetzge.eclipse.aicoder.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import de.hetzge.eclipse.aicoder.AiCoderActivator;

public class MistralPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public MistralPreferencePage() {
		super(GRID);
		setPreferenceStore(AiCoderActivator.getDefault().getPreferenceStore());
		setDescription("Mistral provider settings");
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	protected void createFieldEditors() {
		final StringFieldEditor codestralApiKeyFieldEditor = new StringFieldEditor(
				AiCoderPreferences.CODESTRAL_API_KEY_KEY,
				"Codestral API key:",
				getFieldEditorParent());
		codestralApiKeyFieldEditor.getTextControl(getFieldEditorParent()).setEchoChar('*');
		addField(codestralApiKeyFieldEditor);
	}
}

package de.hetzge.eclipse.aicoder.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import de.hetzge.eclipse.aicoder.AiCoderActivator;

public class InceptionlabsPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public InceptionlabsPreferencePage() {
		super(GRID);
		setPreferenceStore(AiCoderActivator.getDefault().getPreferenceStore());
		setDescription("Inceptionlabs provider settings");
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	protected void createFieldEditors() {
		final StringFieldEditor inceptionlabsApiKeyFieldEditor = new StringFieldEditor(
				AiCoderPreferences.INCEPTIONLABS_API_KEY_KEY,
				"API key:",
				getFieldEditorParent());
		inceptionlabsApiKeyFieldEditor.getTextControl(getFieldEditorParent()).setEchoChar('*');
		addField(inceptionlabsApiKeyFieldEditor);
	}
}

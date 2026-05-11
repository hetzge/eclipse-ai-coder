package de.hetzge.eclipse.aicoder.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import de.hetzge.eclipse.aicoder.AiCoderActivator;

public class OpenRouterPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public OpenRouterPreferencePage() {
		super(GRID);
		setPreferenceStore(AiCoderActivator.getDefault().getPreferenceStore());
		setDescription("OpenRouter provider settings");
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	protected void createFieldEditors() {
		final StringFieldEditor openRouterApiKeyFieldEditor = new StringFieldEditor(
				AiCoderPreferences.OPENROUTER_API_KEY_KEY,
				"API key:",
				getFieldEditorParent());
		openRouterApiKeyFieldEditor.getTextControl(getFieldEditorParent()).setEchoChar('*');
		addField(openRouterApiKeyFieldEditor);
	}
}

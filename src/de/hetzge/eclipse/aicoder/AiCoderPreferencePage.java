package de.hetzge.eclipse.aicoder;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class AiCoderPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public AiCoderPreferencePage() {
		super(GRID);
		setPreferenceStore(AiCoderActivator.getDefault().getPreferenceStore());
		setDescription("AI Code Completion Settings");
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	protected void createFieldEditors() {
		// AI Provider selection
		addField(new RadioGroupFieldEditor(
				AiCoderPreferences.AI_PROVIDER_PREFERENCE_KEY,
				"AI Provider:",
				1,
				new String[][] {
						{ "Mistral", "mistral" }
				},
				getFieldEditorParent()));

		// Mistral settings
		addField(new StringFieldEditor(
				AiCoderPreferences.CODESTRAL_BASE_URL_PREFERENCE_KEY,
				"Codestral Base URL:",
				getFieldEditorParent()));

		final StringFieldEditor codestralApiKeyFieldEditor = new StringFieldEditor(
				AiCoderPreferences.CODESTRAL_API_KEY_PREFERENCE_KEY,
				"Codestral API Key:",
				getFieldEditorParent());
		codestralApiKeyFieldEditor.getTextControl(getFieldEditorParent()).setEchoChar('*');
		addField(codestralApiKeyFieldEditor);

		// Multiline completion setting
		addField(new BooleanFieldEditor(
				AiCoderPreferences.ENABLE_MULTILINE_PREFERENCE_KEY,
				"Enable multiline completion",
				getFieldEditorParent()));
	}
}
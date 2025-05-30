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
				AiCoderPreferences.DEVSTRAL_BASE_URL_PREFERENCE_KEY,
				"Devstral Base URL:",
				getFieldEditorParent()));

		final StringFieldEditor devstralApiKeyFieldEditor = new StringFieldEditor(
				AiCoderPreferences.DEVSTRAL_API_KEY_PREFERENCE_KEY,
				"Devstral API Key:",
				getFieldEditorParent());
		devstralApiKeyFieldEditor.getTextControl(getFieldEditorParent()).setEchoChar('*');
		addField(devstralApiKeyFieldEditor);

		// Multiline completion setting
		addField(new BooleanFieldEditor(
				AiCoderPreferences.ENABLE_MULTILINE_PREFERENCE_KEY,
				"Enable multiline completion",
				getFieldEditorParent()));
	}
}
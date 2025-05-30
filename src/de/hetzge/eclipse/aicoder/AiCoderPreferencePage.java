package de.hetzge.eclipse.aicoder;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Group;
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
		// Create the main selection for AI Provider
		final RadioGroupFieldEditor providerEditor = new RadioGroupFieldEditor(
				AiCoderPreferences.AI_PROVIDER_PREFERENCE_KEY,
				"AI Provider:",
				1,
				new String[][] {
						{ "Mistral", AiProvider.MISTRAL.name() },
						{ "OpenAI", AiProvider.OPENAI.name() }
				},
				getFieldEditorParent());
		addField(providerEditor);

		// Create settings groups right after their respective radio buttons
		final Group mistralGroup = new Group(getFieldEditorParent(), SWT.NONE);
		mistralGroup.setText("Mistral Settings");
		mistralGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		addField(new StringFieldEditor(
				AiCoderPreferences.CODESTRAL_BASE_URL_PREFERENCE_KEY,
				"Codestral Base URL:",
				mistralGroup));

		final StringFieldEditor codestralApiKeyFieldEditor = new StringFieldEditor(
				AiCoderPreferences.CODESTRAL_API_KEY_PREFERENCE_KEY,
				"Codestral API Key:",
				mistralGroup);
		codestralApiKeyFieldEditor.getTextControl(mistralGroup).setEchoChar('*');
		addField(codestralApiKeyFieldEditor);

		// OpenAI settings group
		final Group openaiGroup = new Group(getFieldEditorParent(), SWT.NONE);
		openaiGroup.setText("OpenAI Settings");
		openaiGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		addField(new StringFieldEditor(
				AiCoderPreferences.OPENAI_BASE_URL_PREFERENCE_KEY,
				"OpenAI Base URL:",
				openaiGroup));

		final StringFieldEditor openaiApiKeyFieldEditor = new StringFieldEditor(
				AiCoderPreferences.OPENAI_API_KEY_PREFERENCE_KEY,
				"OpenAI API Key:",
				openaiGroup);
		openaiApiKeyFieldEditor.getTextControl(openaiGroup).setEchoChar('*');
		addField(openaiApiKeyFieldEditor);

		addField(new StringFieldEditor(
				AiCoderPreferences.OPENAI_MODEL_PREFERENCE_KEY,
				"OpenAI Model:",
				openaiGroup));

		// General settings group
		final Group generalGroup = new Group(getFieldEditorParent(), SWT.NONE);
		generalGroup.setText("General Settings");
		generalGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		// Multiline completion setting
		addField(new BooleanFieldEditor(
				AiCoderPreferences.ENABLE_MULTILINE_PREFERENCE_KEY,
				"Enable multiline completion",
				generalGroup));
	}
}
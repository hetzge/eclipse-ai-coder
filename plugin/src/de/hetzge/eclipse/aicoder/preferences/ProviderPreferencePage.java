package de.hetzge.eclipse.aicoder.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.llm.LlmModels;

public class ProviderPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public ProviderPreferencePage() {
		super(GRID);
		setPreferenceStore(AiCoderActivator.getDefault().getPreferenceStore());
		setDescription("AI Coder LLM provider settings");
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	protected void createFieldEditors() {
		// Ollama
		final Group ollamaGroup = new Group(getFieldEditorParent(), SWT.NONE);
		ollamaGroup.setText("Ollama");
		ollamaGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		addField(new StringFieldEditor(
				AiCoderPreferences.OLLAMA_BASE_URL_KEY,
				"Base url:",
				ollamaGroup));

		// Mistral
		final Group mistralGroup = new Group(getFieldEditorParent(), SWT.NONE);
		mistralGroup.setText("Mistral");
		mistralGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		final StringFieldEditor codestralApiKeyFieldEditor = new StringFieldEditor(
				AiCoderPreferences.CODESTRAL_API_KEY_KEY,
				"Codestral API key:",
				mistralGroup);
		codestralApiKeyFieldEditor.getTextControl(mistralGroup).setEchoChar('*');
		addField(codestralApiKeyFieldEditor);

		// OpenAI
		final Group openAiGroup = new Group(getFieldEditorParent(), SWT.NONE);
		openAiGroup.setText("OpenAI");
		openAiGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		addField(new StringFieldEditor(
				AiCoderPreferences.OPENAI_BASE_URL_KEY,
				"Base url:",
				openAiGroup));
		final StringFieldEditor openAiApiKeyFieldEditor = new StringFieldEditor(
				AiCoderPreferences.OPENAI_API_KEY_KEY,
				"API key:",
				openAiGroup);
		openAiApiKeyFieldEditor.getTextControl(openAiGroup).setEchoChar('*');
		addField(openAiApiKeyFieldEditor);
		addField(new StringFieldEditor(
				AiCoderPreferences.OPENAI_FIM_TEMPLATE_KEY,
				"FIM Template:",
				openAiGroup));

		// Inceptionlabs
		final Group inceptionlabsGroup = new Group(getFieldEditorParent(), SWT.NONE);
		inceptionlabsGroup.setText("Inceptionlabs");
		inceptionlabsGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		final StringFieldEditor inceptionlabsApiKeyFieldEditor = new StringFieldEditor(
				AiCoderPreferences.INCEPTIONLABS_API_KEY_KEY,
				"API key:",
				inceptionlabsGroup);
		inceptionlabsApiKeyFieldEditor.getTextControl(inceptionlabsGroup).setEchoChar('*');
		addField(inceptionlabsApiKeyFieldEditor);
	}

	@Override
	protected void performApply() {
		super.performApply();
		LlmModels.INSTANCE.reset();
	}
}

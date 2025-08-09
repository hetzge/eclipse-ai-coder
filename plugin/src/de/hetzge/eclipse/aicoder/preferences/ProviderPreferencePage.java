package de.hetzge.eclipse.aicoder.preferences;

import java.util.Arrays;

import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.llm.LlmModels;
import de.hetzge.eclipse.aicoder.llm.LlmProvider;

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

		// Model selection
		final Group modelGroup = new Group(getFieldEditorParent(), SWT.NONE);
		modelGroup.setText("Model selection");
		modelGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		addField(new ComboFieldEditor(
				AiCoderPreferences.FILL_IN_MIDDLE_PROVIDER_KEY,
				"Fill in middle provider:",
				Arrays.stream(LlmProvider.values())
						.map(provider -> new String[] { provider.name(), provider.name() })
						.toList().toArray(new String[0][]),
				modelGroup));
		addField(new StringFieldEditor(
				AiCoderPreferences.FILL_IN_MIDDLE_MODEL_KEY,
				"Fill in middle model:",
				modelGroup));
		addField(new ComboFieldEditor(
				AiCoderPreferences.EDIT_PROVIDER_KEY,
				"Edit provider:",
				Arrays.stream(LlmProvider.values())
						.map(provider -> new String[] { provider.name(), provider.name() })
						.toList().toArray(new String[0][]),
				modelGroup));
		addField(new StringFieldEditor(
				AiCoderPreferences.EDIT_MODEL_KEY,
				"Edit model:",
				modelGroup));
	}

	@Override
	protected void performApply() {
		super.performApply();
		LlmModels.INSTANCE.reset();
	}
}

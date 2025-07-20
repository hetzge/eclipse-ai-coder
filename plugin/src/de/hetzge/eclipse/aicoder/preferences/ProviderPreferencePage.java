package de.hetzge.eclipse.aicoder.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.AiProvider;

public class ProviderPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private Group ollamaGroup;
	private Group mistralGroup;

	public ProviderPreferencePage() {
		super(GRID);
		setPreferenceStore(AiCoderActivator.getDefault().getPreferenceStore());
		setDescription("AI Coder LLM provider settings");
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	private void updateVisibility(String selectedProvider) {
		this.ollamaGroup.setVisible(AiProvider.OLLAMA.name().equals(selectedProvider));
		((GridData) this.ollamaGroup.getLayoutData()).exclude = !AiProvider.OLLAMA.name().equals(selectedProvider);
		this.mistralGroup.setVisible(AiProvider.MISTRAL.name().equals(selectedProvider));
		((GridData) this.mistralGroup.getLayoutData()).exclude = !AiProvider.MISTRAL.name().equals(selectedProvider);
		getFieldEditorParent().layout(true, true);
	}

	@Override
	protected void createFieldEditors() {
		// Create the main selection for AI Provider
		final RadioGroupFieldEditor providerEditor = new RadioGroupFieldEditor(
				AiCoderPreferences.AI_PROVIDER_KEY,
				"AI Provider:",
				1,
				new String[][] {
						{ "Ollama", AiProvider.OLLAMA.name() },
						{ "Mistral", AiProvider.MISTRAL.name() }
				},
				getFieldEditorParent()) {
			@Override
			protected void fireValueChanged(String property, Object oldValue, Object newValue) {
				super.fireValueChanged(property, oldValue, newValue);
				if ("field_editor_value".equals(property)) {
					updateVisibility((String) newValue);
				}
			}
		};
		addField(providerEditor);

		// Ollama
		this.ollamaGroup = new Group(getFieldEditorParent(), SWT.NONE);
		this.ollamaGroup.setText("Ollama Settings");
		this.ollamaGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		addField(new StringFieldEditor(
				AiCoderPreferences.OLLAMA_BASE_URL_KEY,
				"Ollama Base URL:",
				this.ollamaGroup));

		addField(new StringFieldEditor(
				AiCoderPreferences.OLLAMA_MODEL_KEY,
				"Ollama Model:",
				this.ollamaGroup));

		// Mistral
		this.mistralGroup = new Group(getFieldEditorParent(), SWT.NONE);
		this.mistralGroup.setText("Mistral Settings");
		this.mistralGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		final StringFieldEditor codestralApiKeyFieldEditor = new StringFieldEditor(
				AiCoderPreferences.CODESTRAL_API_KEY,
				"Codestral API Key:",
				this.mistralGroup);
		codestralApiKeyFieldEditor.getTextControl(this.mistralGroup).setEchoChar('*');
		addField(codestralApiKeyFieldEditor);

		// Initialize visibility based on current selection
		updateVisibility(getPreferenceStore().getString(AiCoderPreferences.AI_PROVIDER_KEY));
	}
}

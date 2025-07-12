package de.hetzge.eclipse.aicoder;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class AiCoderPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private Group mistralGroup;
	private Group openaiGroup;

	public AiCoderPreferencePage() {
		super(GRID);
		setPreferenceStore(AiCoderActivator.getDefault().getPreferenceStore());
		setDescription("AI Coder Settings");
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	private void updateVisibility(String selectedProvider) {
		this.mistralGroup.setVisible(AiProvider.MISTRAL.name().equals(selectedProvider));
		((GridData) this.mistralGroup.getLayoutData()).exclude = !AiProvider.MISTRAL.name().equals(selectedProvider);
		this.openaiGroup.setVisible(AiProvider.OPENAI.name().equals(selectedProvider));
		((GridData) this.openaiGroup.getLayoutData()).exclude = !AiProvider.OPENAI.name().equals(selectedProvider);
		getFieldEditorParent().layout(true, true);
	}

	@Override
	protected void createFieldEditors() {
		// General settings group
		final Group generalGroup = new Group(getFieldEditorParent(), SWT.NONE);
		generalGroup.setText("General Settings");
		generalGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		// Multiline completion setting
		addField(new BooleanFieldEditor(
				AiCoderPreferences.ENABLE_MULTILINE_PREFERENCE_KEY,
				"Enable multiline completion",
				generalGroup));

		// Autocomplete settings
		addField(new BooleanFieldEditor(
				AiCoderPreferences.ENABLE_AUTOCOMPLETE_KEY,
				"Enable autocomplete",
				generalGroup));

		// Ignore jre/jdk classes
		final BooleanFieldEditor ignoreJreClassesEditor = new BooleanFieldEditor(
				AiCoderPreferences.IGNORE_JRE_CLASSES_KEY,
				"Ignore JRE classes",
				generalGroup);
		addField(ignoreJreClassesEditor);

		// Context size settings
		final IntegerFieldEditor maxPrefixSizeEditor = new IntegerFieldEditor(
				AiCoderPreferences.MAX_PREFIX_SIZE_KEY,
				"Maximum prefix size (lines):",
				generalGroup);
		maxPrefixSizeEditor.setValidRange(0, 10000);
		addField(maxPrefixSizeEditor);

		final IntegerFieldEditor maxSuffixSizeEditor = new IntegerFieldEditor(
				AiCoderPreferences.MAX_SUFFIX_SIZE_KEY,
				"Maximum suffix size (lines):",
				generalGroup);
		maxSuffixSizeEditor.setValidRange(0, 10000);
		addField(maxSuffixSizeEditor);

		final IntegerFieldEditor maxTokensEditor = new IntegerFieldEditor(
				AiCoderPreferences.MAX_TOKENS_KEY,
				"Maximum tokens:",
				generalGroup);
		maxTokensEditor.setValidRange(0, 10000);
		addField(maxTokensEditor);

		final IntegerFieldEditor debounceInMsEditor = new IntegerFieldEditor(
				AiCoderPreferences.DEBOUNCE_IN_MS_KEY,
				"Debounce (in ms):",
				generalGroup);
		debounceInMsEditor.setValidRange(0, 10000);
		addField(debounceInMsEditor);

		// Create the main selection for AI Provider
		final RadioGroupFieldEditor providerEditor = new RadioGroupFieldEditor(
				AiCoderPreferences.AI_PROVIDER_PREFERENCE_KEY,
				"AI Provider:",
				1,
				new String[][] {
						{ "Mistral", AiProvider.MISTRAL.name() },
						{ "OpenAI", AiProvider.OPENAI.name() }
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

		// Create settings groups right after their respective radio buttons
		this.mistralGroup = new Group(getFieldEditorParent(), SWT.NONE);
		this.mistralGroup.setText("Mistral Settings");
		this.mistralGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		addField(new StringFieldEditor(
				AiCoderPreferences.CODESTRAL_BASE_URL_PREFERENCE_KEY,
				"Codestral Base URL:",
				this.mistralGroup));

		final StringFieldEditor codestralApiKeyFieldEditor = new StringFieldEditor(
				AiCoderPreferences.CODESTRAL_API_KEY_PREFERENCE_KEY,
				"Codestral API Key:",
				this.mistralGroup);
		codestralApiKeyFieldEditor.getTextControl(this.mistralGroup).setEchoChar('*');
		addField(codestralApiKeyFieldEditor);

		// OpenAI settings group
		this.openaiGroup = new Group(getFieldEditorParent(), SWT.NONE);
		this.openaiGroup.setText("OpenAI Settings");
		this.openaiGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		addField(new StringFieldEditor(
				AiCoderPreferences.OPENAI_BASE_URL_PREFERENCE_KEY,
				"OpenAI Base URL:",
				this.openaiGroup));

		final StringFieldEditor openaiApiKeyFieldEditor = new StringFieldEditor(
				AiCoderPreferences.OPENAI_API_KEY_PREFERENCE_KEY,
				"OpenAI API Key:",
				this.openaiGroup);
		openaiApiKeyFieldEditor.getTextControl(this.openaiGroup).setEchoChar('*');
		addField(openaiApiKeyFieldEditor);

		addField(new StringFieldEditor(
				AiCoderPreferences.OPENAI_MODEL_PREFERENCE_KEY,
				"OpenAI Model:",
				this.openaiGroup));

		// Initialize visibility based on current selection
		updateVisibility(getPreferenceStore().getString(AiCoderPreferences.AI_PROVIDER_PREFERENCE_KEY));
	}
}
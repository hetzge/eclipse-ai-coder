package de.hetzge.eclipse.aicoder.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import de.hetzge.eclipse.aicoder.AiCoderActivator;

public abstract class AbstractOpenAiPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private final String baseUrlPreferenceKey;
	private final String apiKeyPreferenceKey;

	public AbstractOpenAiPreferencePage(String description, String baseUrlPreferenceKey, String apiKeyPreferenceKey) {
		super(GRID);
		setPreferenceStore(AiCoderActivator.getDefault().getPreferenceStore());
		setDescription(description);
		this.baseUrlPreferenceKey = baseUrlPreferenceKey;
		this.apiKeyPreferenceKey = apiKeyPreferenceKey;
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	protected void createFieldEditors() {
		addField(new StringFieldEditor(
				this.baseUrlPreferenceKey,
				"Base url:",
				getFieldEditorParent()));
		final StringFieldEditor apiKeyFieldEditor = new StringFieldEditor(
				this.apiKeyPreferenceKey,
				"API key:",
				getFieldEditorParent());
		apiKeyFieldEditor.getTextControl(getFieldEditorParent()).setEchoChar('*');
		addField(apiKeyFieldEditor);
	}
}

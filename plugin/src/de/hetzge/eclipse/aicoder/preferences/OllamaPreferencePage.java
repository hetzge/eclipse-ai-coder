package de.hetzge.eclipse.aicoder.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import de.hetzge.eclipse.aicoder.AiCoderActivator;

public class OllamaPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public OllamaPreferencePage() {
		super(GRID);
		setPreferenceStore(AiCoderActivator.getDefault().getPreferenceStore());
		setDescription("Ollama provider settings");
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	protected void createFieldEditors() {
		addField(new StringFieldEditor(
				AiCoderPreferences.OLLAMA_BASE_URL_KEY,
				"Base url:",
				getFieldEditorParent()));
		addField(new IntegerFieldEditor(
				AiCoderPreferences.OLLAMA_NUM_CTX_KEY,
				"Context size (num_ctx):",
				getFieldEditorParent()));
	}
}

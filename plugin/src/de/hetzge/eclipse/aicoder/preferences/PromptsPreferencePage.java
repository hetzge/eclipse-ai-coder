package de.hetzge.eclipse.aicoder.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import de.hetzge.eclipse.aicoder.AiCoderActivator;

public class PromptsPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public PromptsPreferencePage() {
		super(GRID);
		setPreferenceStore(AiCoderActivator.getDefault().getPreferenceStore());
		setDescription("AI Coder prompt settings");
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	protected void createFieldEditors() {
		addField(new StringFieldEditor(AiCoderPreferences.QUICK_FIX_PROMPT_KEY, "Quick fix prompt", 70, 7, StringFieldEditor.VALIDATE_ON_KEY_STROKE, getFieldEditorParent()));
		addField(new StringFieldEditor(AiCoderPreferences.CHANGE_CODE_SYSTEM_PROMPT_KEY, "Change code system prompt", 70, 7, StringFieldEditor.VALIDATE_ON_KEY_STROKE, getFieldEditorParent()));
		addField(new StringFieldEditor(AiCoderPreferences.GENERATE_CODE_SYSTEM_PROMPT_KEY, "Generate code system prompt", 70, 7, StringFieldEditor.VALIDATE_ON_KEY_STROKE, getFieldEditorParent()));
    addField(new StringFieldEditor(AiCoderPreferences.PSEUDO_FIM_SYSTEM_PROMPT_KEY, "Pseduo FIM system prompt", 70, 7, StringFieldEditor.VALIDATE_ON_KEY_STROKE, getFieldEditorParent()));
	}
}

package de.hetzge.eclipse.aicoder.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
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
		// Provider configuration moved to subpages
	}

	@Override
	protected void performApply() {
		super.performApply();
		LlmModels.INSTANCE.reset();
	}
}

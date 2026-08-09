package de.hetzge.eclipse.aicoder.preferences;

import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.widgets.Composite;

import de.hetzge.eclipse.aicoder.context.FillInMiddleContextEntry;

public class FillInMiddleContextPreferencePage extends ContextTypePreferencePage {

	public static final String ID = "de.hetzge.eclipse.aicoder.preferences.context.fill_in_middle";
	public static final String CONTEXT_PREFIX = FillInMiddleContextEntry.PREFIX;

	public FillInMiddleContextPreferencePage() {
		super(FillInMiddleContextEntry.PREFIX);
	}

	@Override
	protected void createFieldEditors(Composite parent) {
		final IntegerFieldEditor prefixLineCountEditor = new IntegerFieldEditor(
				AiCoderPreferences.MAX_PREFIX_SIZE_KEY,
				"Prefix line count:",
				parent);
		prefixLineCountEditor.setValidRange(0, 1000000);
		addField(prefixLineCountEditor, parent);

		final IntegerFieldEditor suffixLineCountEditor = new IntegerFieldEditor(
				AiCoderPreferences.MAX_SUFFIX_SIZE_KEY,
				"Suffix line count:",
				parent);
		suffixLineCountEditor.setValidRange(0, 1000000);
		addField(suffixLineCountEditor, parent);
	}
}

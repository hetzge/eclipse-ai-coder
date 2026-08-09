package de.hetzge.eclipse.aicoder.preferences;

import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.widgets.Composite;

import de.hetzge.eclipse.aicoder.context.CodeViewportMemoryContextEntry;

public class CodeViewportMemoryContextPreferencePage extends ContextTypePreferencePage {

	public static final String ID = "de.hetzge.eclipse.aicoder.preferences.context.code_viewport_memory";
	public static final String CONTEXT_PREFIX = CodeViewportMemoryContextEntry.PREFIX;

	public CodeViewportMemoryContextPreferencePage() {
		super(CodeViewportMemoryContextEntry.PREFIX);
	}

	@Override
	protected void createFieldEditors(Composite parent) {
		final IntegerFieldEditor maxLinesEditor = new IntegerFieldEditor(
				AiCoderPreferences.CODE_VIEWPORT_MEMORY_MAX_LINES_KEY,
				"Max lines:",
				parent);
		maxLinesEditor.setValidRange(0, 1000000);
		addField(maxLinesEditor, parent);
	}
}

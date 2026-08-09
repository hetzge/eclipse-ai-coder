package de.hetzge.eclipse.aicoder.preferences;

import de.hetzge.eclipse.aicoder.context.CodeViewportMemoryContextEntry;

public class CodeViewportMemoryContextPreferencePage extends ContextTypePreferencePage {

	public static final String ID = "de.hetzge.eclipse.aicoder.preferences.context.code_viewport_memory";
	public static final String CONTEXT_PREFIX = CodeViewportMemoryContextEntry.PREFIX;

	public CodeViewportMemoryContextPreferencePage() {
		super(CodeViewportMemoryContextEntry.PREFIX);
	}
}

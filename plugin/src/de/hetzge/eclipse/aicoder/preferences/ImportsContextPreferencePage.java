package de.hetzge.eclipse.aicoder.preferences;

import de.hetzge.eclipse.aicoder.context.ImportsContextEntry;

public class ImportsContextPreferencePage extends ContextTypePreferencePage {

	public static final String ID = "de.hetzge.eclipse.aicoder.preferences.context.imports";
	public static final String CONTEXT_PREFIX = ImportsContextEntry.PREFIX;

	public ImportsContextPreferencePage() {
		super(ImportsContextEntry.PREFIX);
	}
}

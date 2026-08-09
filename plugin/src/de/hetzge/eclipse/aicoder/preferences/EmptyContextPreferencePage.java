package de.hetzge.eclipse.aicoder.preferences;

import de.hetzge.eclipse.aicoder.context.EmptyContextEntry;

public class EmptyContextPreferencePage extends ContextTypePreferencePage {

	public static final String ID = "de.hetzge.eclipse.aicoder.preferences.context.empty";
	public static final String CONTEXT_PREFIX = EmptyContextEntry.PREFIX;

	public EmptyContextPreferencePage() {
		super(EmptyContextEntry.PREFIX);
	}
}

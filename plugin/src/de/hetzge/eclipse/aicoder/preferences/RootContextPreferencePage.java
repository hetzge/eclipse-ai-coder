package de.hetzge.eclipse.aicoder.preferences;

import de.hetzge.eclipse.aicoder.context.RootContextEntry;

public class RootContextPreferencePage extends ContextTypePreferencePage {

	public static final String ID = "de.hetzge.eclipse.aicoder.preferences.context.root";
	public static final String CONTEXT_PREFIX = RootContextEntry.PREFIX;

	public RootContextPreferencePage() {
		super(RootContextEntry.PREFIX);
	}
}

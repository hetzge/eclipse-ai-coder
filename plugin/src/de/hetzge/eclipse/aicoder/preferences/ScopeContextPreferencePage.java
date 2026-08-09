package de.hetzge.eclipse.aicoder.preferences;

import de.hetzge.eclipse.aicoder.context.ScopeContextEntry;

public class ScopeContextPreferencePage extends ContextTypePreferencePage {

	public static final String ID = "de.hetzge.eclipse.aicoder.preferences.context.scope";
	public static final String CONTEXT_PREFIX = ScopeContextEntry.PREFIX;

	public ScopeContextPreferencePage() {
		super(ScopeContextEntry.PREFIX);
	}
}

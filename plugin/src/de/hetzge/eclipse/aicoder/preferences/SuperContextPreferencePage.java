package de.hetzge.eclipse.aicoder.preferences;

import de.hetzge.eclipse.aicoder.context.SuperContextEntry;

public class SuperContextPreferencePage extends ContextTypePreferencePage {

	public static final String ID = "de.hetzge.eclipse.aicoder.preferences.context.super";
	public static final String CONTEXT_PREFIX = SuperContextEntry.PREFIX;

	public SuperContextPreferencePage() {
		super(SuperContextEntry.PREFIX);
	}
}

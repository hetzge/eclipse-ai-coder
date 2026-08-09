package de.hetzge.eclipse.aicoder.preferences;

import de.hetzge.eclipse.aicoder.context.CustomContextEntry;

public class CustomContextPreferencePage extends ContextTypePreferencePage {

	public static final String ID = "de.hetzge.eclipse.aicoder.preferences.context.custom";
	public static final String CONTEXT_PREFIX = CustomContextEntry.PREFIX;

	public CustomContextPreferencePage() {
		super(CustomContextEntry.PREFIX);
	}
}

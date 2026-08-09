package de.hetzge.eclipse.aicoder.preferences;

import de.hetzge.eclipse.aicoder.context.BlacklistedContextEntry;

public class BlacklistedContextPreferencePage extends ContextTypePreferencePage {

	public static final String ID = "de.hetzge.eclipse.aicoder.preferences.context.blacklisted";
	public static final String CONTEXT_PREFIX = BlacklistedContextEntry.PREFIX;

	public BlacklistedContextPreferencePage() {
		super(BlacklistedContextEntry.PREFIX);
	}
}

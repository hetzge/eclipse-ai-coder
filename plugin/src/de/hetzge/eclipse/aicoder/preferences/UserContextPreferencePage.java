package de.hetzge.eclipse.aicoder.preferences;

import de.hetzge.eclipse.aicoder.context.UserContextEntry;

public class UserContextPreferencePage extends ContextTypePreferencePage {

	public static final String ID = "de.hetzge.eclipse.aicoder.preferences.context.user";
	public static final String CONTEXT_PREFIX = UserContextEntry.PREFIX;

	public UserContextPreferencePage() {
		super(UserContextEntry.PREFIX);
	}
}

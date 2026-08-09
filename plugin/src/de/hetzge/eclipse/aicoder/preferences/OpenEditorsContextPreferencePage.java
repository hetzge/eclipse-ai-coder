package de.hetzge.eclipse.aicoder.preferences;

import de.hetzge.eclipse.aicoder.context.OpenEditorsContextEntry;

public class OpenEditorsContextPreferencePage extends ContextTypePreferencePage {

	public static final String ID = "de.hetzge.eclipse.aicoder.preferences.context.open_editors";
	public static final String CONTEXT_PREFIX = OpenEditorsContextEntry.PREFIX;

	public OpenEditorsContextPreferencePage() {
		super(OpenEditorsContextEntry.PREFIX);
	}
}

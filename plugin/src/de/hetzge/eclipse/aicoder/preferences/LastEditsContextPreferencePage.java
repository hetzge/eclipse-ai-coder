package de.hetzge.eclipse.aicoder.preferences;

import de.hetzge.eclipse.aicoder.context.LastEditsContextEntry;

public class LastEditsContextPreferencePage extends ContextTypePreferencePage {

	public static final String ID = "de.hetzge.eclipse.aicoder.preferences.context.last_edits";
	public static final String CONTEXT_PREFIX = LastEditsContextEntry.PREFIX;

	public LastEditsContextPreferencePage() {
		super(LastEditsContextEntry.PREFIX);
	}
}

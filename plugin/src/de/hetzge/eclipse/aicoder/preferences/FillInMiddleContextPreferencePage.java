package de.hetzge.eclipse.aicoder.preferences;

import de.hetzge.eclipse.aicoder.context.FillInMiddleContextEntry;

public class FillInMiddleContextPreferencePage extends ContextTypePreferencePage {

	public static final String ID = "de.hetzge.eclipse.aicoder.preferences.context.fill_in_middle";
	public static final String CONTEXT_PREFIX = FillInMiddleContextEntry.PREFIX;

	public FillInMiddleContextPreferencePage() {
		super(FillInMiddleContextEntry.PREFIX);
	}
}

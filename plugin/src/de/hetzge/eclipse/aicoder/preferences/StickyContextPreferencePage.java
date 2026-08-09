package de.hetzge.eclipse.aicoder.preferences;

import de.hetzge.eclipse.aicoder.context.StickyContextEntry;

public class StickyContextPreferencePage extends ContextTypePreferencePage {

	public static final String ID = "de.hetzge.eclipse.aicoder.preferences.context.sticky";
	public static final String CONTEXT_PREFIX = StickyContextEntry.PREFIX;

	public StickyContextPreferencePage() {
		super(StickyContextEntry.PREFIX);
	}
}

package de.hetzge.eclipse.aicoder.preferences;

import de.hetzge.eclipse.aicoder.context.ScratchpadContextEntry;

public class ScratchpadContextPreferencePage extends ContextTypePreferencePage {

	public static final String ID = "de.hetzge.eclipse.aicoder.preferences.context.scratchpad";
	public static final String CONTEXT_PREFIX = ScratchpadContextEntry.PREFIX;

	public ScratchpadContextPreferencePage() {
		super(ScratchpadContextEntry.PREFIX);
	}
}

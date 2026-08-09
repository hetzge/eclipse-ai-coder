package de.hetzge.eclipse.aicoder.preferences;

import de.hetzge.eclipse.aicoder.context.ClipboardContextEntry;

public class ClipboardContextPreferencePage extends ContextTypePreferencePage {

	public static final String ID = "de.hetzge.eclipse.aicoder.preferences.context.clipboard";
	public static final String CONTEXT_PREFIX = ClipboardContextEntry.PREFIX;

	public ClipboardContextPreferencePage() {
		super(ClipboardContextEntry.PREFIX);
	}
}

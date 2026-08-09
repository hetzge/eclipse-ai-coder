package de.hetzge.eclipse.aicoder.preferences;

import de.hetzge.eclipse.aicoder.context.TypeContextEntry;

public class TypeContextPreferencePage extends ContextTypePreferencePage {

	public static final String ID = "de.hetzge.eclipse.aicoder.preferences.context.type";
	public static final String CONTEXT_PREFIX = TypeContextEntry.PREFIX;

	public TypeContextPreferencePage() {
		super(TypeContextEntry.PREFIX);
	}
}

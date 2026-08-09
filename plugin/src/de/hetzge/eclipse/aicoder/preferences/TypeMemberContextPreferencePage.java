package de.hetzge.eclipse.aicoder.preferences;

import de.hetzge.eclipse.aicoder.context.TypeMemberContextEntry;

public class TypeMemberContextPreferencePage extends ContextTypePreferencePage {

	public static final String ID = "de.hetzge.eclipse.aicoder.preferences.context.type_member";
	public static final String CONTEXT_PREFIX = TypeMemberContextEntry.PREFIX;

	public TypeMemberContextPreferencePage() {
		super(TypeMemberContextEntry.PREFIX);
	}
}

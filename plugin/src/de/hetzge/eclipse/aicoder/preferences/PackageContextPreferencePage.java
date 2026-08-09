package de.hetzge.eclipse.aicoder.preferences;

import de.hetzge.eclipse.aicoder.context.PackageContextEntry;

public class PackageContextPreferencePage extends ContextTypePreferencePage {

	public static final String ID = "de.hetzge.eclipse.aicoder.preferences.context.package";
	public static final String CONTEXT_PREFIX = PackageContextEntry.PREFIX;

	public PackageContextPreferencePage() {
		super(PackageContextEntry.PREFIX);
	}
}

package de.hetzge.eclipse.aicoder.preferences;

import de.hetzge.eclipse.aicoder.context.FileTreeContextEntry;

public class FileTreeContextPreferencePage extends ContextTypePreferencePage {

	public static final String ID = "de.hetzge.eclipse.aicoder.preferences.context.file_tree";
	public static final String CONTEXT_PREFIX = FileTreeContextEntry.PREFIX;

	public FileTreeContextPreferencePage() {
		super(FileTreeContextEntry.PREFIX);
	}
}

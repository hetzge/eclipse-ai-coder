package de.hetzge.eclipse.aicoder.preferences;

import org.eclipse.swt.widgets.Composite;

import de.hetzge.eclipse.aicoder.context.FileTreeContextEntry;

public class FileTreeContextPreferencePage extends ContextTypePreferencePage {

	public static final String ID = "de.hetzge.eclipse.aicoder.preferences.context.file_tree";
	public static final String CONTEXT_PREFIX = FileTreeContextEntry.PREFIX;

	public FileTreeContextPreferencePage() {
		super(FileTreeContextEntry.PREFIX);
	}

	@Override
	protected void createFieldEditors(Composite parent) {
		addField(new StringListFieldEditor(AiCoderPreferences.FILE_TREE_WHITELIST_KEY, "Whitelist:", parent), parent);
		addField(new StringListFieldEditor(AiCoderPreferences.FILE_TREE_BLACKLIST_KEY, "Blacklist:", parent), parent);
	}
}

package de.hetzge.eclipse.aicoder.preferences;

import org.eclipse.swt.widgets.Composite;

import de.hetzge.eclipse.aicoder.context.AiRerankContextEntry;

public class AiRerankContextPreferencePage extends ContextTypePreferencePage {

	public static final String ID = "de.hetzge.eclipse.aicoder.preferences.context.ai_rerank";
	public static final String CONTEXT_PREFIX = AiRerankContextEntry.PREFIX;

	public AiRerankContextPreferencePage() {
		super(AiRerankContextEntry.PREFIX);
	}

	@Override
	protected void createFieldEditors(Composite parent) {
		addField(new StringListFieldEditor(AiCoderPreferences.AI_RERANK_WHITELIST_KEY, "Whitelist:", parent), parent);
		addField(new StringListFieldEditor(AiCoderPreferences.AI_RERANK_BLACKLIST_KEY, "Blacklist:", parent), parent);
	}
}

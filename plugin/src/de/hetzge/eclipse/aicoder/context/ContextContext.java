package de.hetzge.eclipse.aicoder.context;

import java.util.HashSet;
import java.util.Set;

import de.hetzge.eclipse.aicoder.preferences.ContextPreferences;

public class ContextContext {
	private final Set<ContextEntryKey> doneKeys;
	private final ContextPreferences preferences;

	public ContextContext(ContextPreferences preferences) {
		this.doneKeys = new HashSet<>();
		this.preferences = preferences;
	}

	public boolean isDone(ContextEntry entry) {
		return this.doneKeys.contains(entry.getKey());
	}

	public void markDone(ContextEntry entry) {
		this.doneKeys.add(entry.getKey());
	}

	public ContextPreferences getPreferences() {
		return this.preferences;
	}
}

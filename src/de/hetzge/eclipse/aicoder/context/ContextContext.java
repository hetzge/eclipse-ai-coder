package de.hetzge.eclipse.aicoder.context;

import java.util.HashSet;
import java.util.Set;

public class ContextContext {
	private final Set<ContextEntryKey> doneKeys;

	public ContextContext() {
		this.doneKeys = new HashSet<>();
	}

	public boolean isDone(ContextEntry entry) {
		return this.doneKeys.contains(entry.getKey());
	}

	public void markDone(ContextEntry entry) {
		this.doneKeys.add(entry.getKey());
	}
}
package de.hetzge.eclipse.aicoder.context;

import java.time.Duration;
import java.util.List;

import de.hetzge.eclipse.aicoder.preferences.ContextPreferences;

public class UserContextEntry extends ContextEntry {
	public static final String PREFIX = "USER";

	private UserContextEntry(List<CustomContextEntry> childContextEntries, Duration creationDuration) {
		super(childContextEntries, creationDuration);
	}

	@Override
	public ContextEntryKey getKey() {
		return new ContextEntryKey(PREFIX, PREFIX);
	}

	@Override
	public String getLabel() {
		return "Custom";
	}

	@Override
	public String getContent(ContextContext context) {
		return super.getContent(context) + "\n";
	}

	public static UserContextEntry create() {
		final long before = System.currentTimeMillis();
		final List<CustomContextEntry> entries = ContextPreferences.getCustomContextEntries();
		return new UserContextEntry(entries, Duration.ofMillis(System.currentTimeMillis() - before));
	}
}
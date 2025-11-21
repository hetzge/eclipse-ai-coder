package de.hetzge.eclipse.aicoder.context;

import java.nio.file.Path;
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

	public static ContextEntryFactory factory(Path path) {
		return new ContextEntryFactory(PREFIX, () -> create(path));
	}

	public static UserContextEntry create(Path path) {
		final long before = System.currentTimeMillis();
		final List<CustomContextEntry> entries = ContextPreferences.getCustomContextEntryDatas().stream()
				.map(data -> new CustomContextEntry(data, data.matches(path)))
				.toList();
		return new UserContextEntry(entries, Duration.ofMillis(System.currentTimeMillis() - before));
	}
}
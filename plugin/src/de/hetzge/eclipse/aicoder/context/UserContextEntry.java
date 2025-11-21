package de.hetzge.eclipse.aicoder.context;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import de.hetzge.eclipse.aicoder.preferences.ContextPreferences;

public class UserContextEntry extends ContextEntry {
	public static final String PREFIX = "USER";
	private final Path path;

	private UserContextEntry(List<CustomContextEntry> childContextEntries, Path path, Duration creationDuration) {
		super(childContextEntries, creationDuration);
		this.path = path;
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
		return this.childContextEntries.stream()
				.filter(entry -> ((CustomContextEntry) entry).matches(this.path))
				.map(entry -> apply(entry, context))
				.collect(Collectors.joining()) + "\n";
	}

	public static ContextEntryFactory factory(Path path) {
		return new ContextEntryFactory(PREFIX, () -> create(path));
	}

	public static UserContextEntry create(Path path) {
		final long before = System.currentTimeMillis();
		final List<CustomContextEntry> entries = ContextPreferences.getCustomContextEntries();
		return new UserContextEntry(entries, path, Duration.ofMillis(System.currentTimeMillis() - before));
	}
}
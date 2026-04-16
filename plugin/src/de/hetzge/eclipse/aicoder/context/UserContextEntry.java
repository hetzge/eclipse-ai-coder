package de.hetzge.eclipse.aicoder.context;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import de.hetzge.eclipse.aicoder.config.ContextConfig.UserConfig;
import de.hetzge.eclipse.aicoder.config.TaskConfig;
import de.hetzge.eclipse.aicoder.preferences.ContextPreferences;

public class UserContextEntry extends ContextEntry {

	public static final String LABEL = "Custom";
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
		return LABEL;
	}

	public static ContextEntryFactory factory(Path path, TaskConfig config) {
		return new ContextEntryFactory(PREFIX, () -> create(path, config), () -> new EmptyContextEntry(PREFIX, LABEL, null));
	}

	public static UserContextEntry create(Path path, TaskConfig config) {
		final long before = System.currentTimeMillis();
		final List<CustomContextEntry> entries = new ArrayList<>();
		entries.addAll(ContextPreferences.getCustomContextEntryDatas().stream()
				.map(data -> new CustomContextEntry(data, data.matches(path)))
				.toList());
		entries.addAll(config.getContextConfig(PREFIX)
				.map(UserConfig.class::cast)
				.map(UserConfig::getEntries)
				.orElse(List.of()).stream()
				.map(data -> new CustomContextEntry(data, data.matches(path)))
				.toList());
		return new UserContextEntry(entries, Duration.ofMillis(System.currentTimeMillis() - before));
	}
}
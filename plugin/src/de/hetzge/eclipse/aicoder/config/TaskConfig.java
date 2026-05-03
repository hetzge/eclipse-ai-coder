package de.hetzge.eclipse.aicoder.config;

import java.util.List;
import java.util.Optional;

import org.tomlj.Toml;
import org.tomlj.TomlTable;

public record TaskConfig(TomlTable table) {

	public List<ContextConfig> getContextConfigs() {
		return this.table.getArrayOrEmpty("context").toList().stream()
				.map(TomlTable.class::cast)
				.map(ContextConfig::create)
				.toList();
	}

	public Optional<ContextConfig> getContextConfig(String key) {
		return this.table.getArrayOrEmpty("context").toList().stream()
				.filter(it -> (it instanceof final TomlTable table) && table.getString("key", () -> "").equals(key))
				.map(TomlTable.class::cast)
				.map(ContextConfig::create)
				.findFirst();
	}

	public ContextConfig getContextConfigOrDefault(String key) {
		return getContextConfig(key).orElseGet(() -> ContextConfig.createDefault(key));
	}

	public boolean hasContextConfig() {
		return !this.table.getArrayOrEmpty("context").isEmpty();
	}

	public static TaskConfig createDefault() {
		final TomlTable table = Toml.parse("context = []");
		return new TaskConfig(table);
	}
}

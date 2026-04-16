package de.hetzge.eclipse.aicoder.config;

import java.util.Optional;

import org.tomlj.Toml;
import org.tomlj.TomlTable;

import de.hetzge.eclipse.aicoder.CompletionMode;

// TODO enable/disable context by glob
// TODO global java glob whitelist/blacklist per project
// TODO global config (in user/workspace folder)

public record RootConfig(TomlTable table) {

	public Optional<TaskConfig> getTaskConfig(CompletionMode mode) {
		if (mode == CompletionMode.EDIT) {
			return Optional.of(new TaskConfig(this.table.getTable("edit")));
		} else if (mode == CompletionMode.QUICK_FIX) {
			return Optional.of(new TaskConfig(this.table.getTable("quickfix")));
		} else if (mode == CompletionMode.GENERATE) {
			return Optional.of(new TaskConfig(this.table.getTable("generate")));
		} else if (mode == CompletionMode.INLINE) {
			return Optional.of(new TaskConfig(this.table.getTable("fim")));
		} else if (mode == CompletionMode.NEXT_EDIT) {
			return Optional.of(new TaskConfig(this.table.getTable("next_edit")));
		} else {
			return Optional.empty();
		}
	}

	public TaskConfig getFimConfig() {
		return new TaskConfig(this.table.getTable("fim"));
	}

	public TaskConfig getQuickFixConfig() {
		return new TaskConfig(this.table.getTable("quickfix"));
	}

	public TaskConfig getGenerateConfig() {
		return new TaskConfig(this.table.getTable("generate"));
	}

	public TaskConfig getEditConfig() {
		return new TaskConfig(this.table.getTable("edit"));
	}

	public TaskConfig getNextEditConfig() {
		return new TaskConfig(this.table.getTable("next_edit"));
	}

	public TaskConfig getRerankConfig() {
		return new TaskConfig(this.table.getTable("rerank"));
	}

	public static RootConfig parse(String tomlContent) {
		return new RootConfig(Toml.parse(tomlContent));
	}
}

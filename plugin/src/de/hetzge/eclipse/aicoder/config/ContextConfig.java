package de.hetzge.eclipse.aicoder.config;

import java.util.List;

import org.tomlj.Toml;
import org.tomlj.TomlTable;

import de.hetzge.eclipse.aicoder.context.AiRerankContextEntry;
import de.hetzge.eclipse.aicoder.context.BlacklistedContextEntry;
import de.hetzge.eclipse.aicoder.context.ClipboardContextEntry;
import de.hetzge.eclipse.aicoder.context.CodeViewportMemoryContextEntry;
import de.hetzge.eclipse.aicoder.context.CustomContextEntry;
import de.hetzge.eclipse.aicoder.context.CustomContextEntryData;
import de.hetzge.eclipse.aicoder.context.EmptyContextEntry;
import de.hetzge.eclipse.aicoder.context.FileTreeContextEntry;
import de.hetzge.eclipse.aicoder.context.FillInMiddleContextEntry;
import de.hetzge.eclipse.aicoder.context.ImportsContextEntry;
import de.hetzge.eclipse.aicoder.context.LastEditsContextEntry;
import de.hetzge.eclipse.aicoder.context.OpenEditorsContextEntry;
import de.hetzge.eclipse.aicoder.context.PackageContextEntry;
import de.hetzge.eclipse.aicoder.context.ProjectInformationContextEntry;
import de.hetzge.eclipse.aicoder.context.RootContextEntry;
import de.hetzge.eclipse.aicoder.context.ScopeContextEntry;
import de.hetzge.eclipse.aicoder.context.StickyContextEntry;
import de.hetzge.eclipse.aicoder.context.SuperContextEntry;
import de.hetzge.eclipse.aicoder.context.TypeContextEntry;
import de.hetzge.eclipse.aicoder.context.TypeMemberContextEntry;
import de.hetzge.eclipse.aicoder.context.UserContextEntry;
import de.hetzge.eclipse.aicoder.preferences.AiCoderPreferences;

public sealed interface ContextConfig {

	String key();

	public static ContextConfig createDefault(String key) {
		return create(Toml.parse("key = \"" + key + "\""));
	}

	public static ContextConfig create(TomlTable table) {
		return switch (table.getString("key")) {
		case ProjectInformationContextEntry.PREFIX -> new ProjectInformationsConfig(table, ProjectInformationContextEntry.PREFIX);
		case FileTreeContextEntry.PREFIX -> new FileTreeConfig(table, FileTreeContextEntry.PREFIX);
		case OpenEditorsContextEntry.PREFIX -> new OpenEditorsConfig(table, OpenEditorsContextEntry.PREFIX);
		case ImportsContextEntry.PREFIX -> new ImportsConfig(table, ImportsContextEntry.PREFIX);
		case SuperContextEntry.PREFIX -> new SuperConfig(table, SuperContextEntry.PREFIX);
		case StickyContextEntry.PREFIX -> new StickyConfig(table, StickyContextEntry.PREFIX);
		case TypeContextEntry.PREFIX -> new TypeConfig(table, TypeContextEntry.PREFIX);
		case FillInMiddleContextEntry.PREFIX -> new FillInMiddleConfig(table, FillInMiddleContextEntry.PREFIX);
		case CustomContextEntry.PREFIX -> new CustomConfig(table, CustomContextEntry.PREFIX);
		case ClipboardContextEntry.PREFIX -> new ClipboardConfig(table, ClipboardContextEntry.PREFIX);
		case LastEditsContextEntry.PREFIX -> new LastEditsConfig(table, LastEditsContextEntry.PREFIX);
		case EmptyContextEntry.PREFIX -> new EmptyConfig(table, EmptyContextEntry.PREFIX);
		case BlacklistedContextEntry.PREFIX -> new BlacklistedConfig(table, BlacklistedContextEntry.PREFIX);
		case ScopeContextEntry.PREFIX -> new ScopeConfig(table, ScopeContextEntry.PREFIX);
		case UserContextEntry.PREFIX -> new UserConfig(table, UserContextEntry.PREFIX);
		case RootContextEntry.PREFIX -> new RootConfig(table, RootContextEntry.PREFIX);
		case TypeMemberContextEntry.PREFIX -> new TypeMemberConfig(table, TypeMemberContextEntry.PREFIX);
		case PackageContextEntry.PREFIX -> new PackageConfig(table, PackageContextEntry.PREFIX);
		case AiRerankContextEntry.PREFIX -> new AiRerankConfig(table, AiRerankContextEntry.PREFIX);
		case CodeViewportMemoryContextEntry.PREFIX -> new CodeViewportMemoryConfig(table, CodeViewportMemoryContextEntry.PREFIX);
		default -> throw new IllegalArgumentException("Unknown context type: " + table.getString("key"));
		};
	}

	public record ProjectInformationsConfig(TomlTable table, String key) implements ContextConfig {
		public List<String> getWhitelist() {
			return this.table.getArrayOrEmpty("whitelist").toList().stream().map(String.class::cast).toList();
		}
	}

	public record FileTreeConfig(TomlTable table, String key) implements ContextConfig {
		public List<String> getWhitelist() {
			return this.table.getArrayOrEmpty("whitelist").toList().stream().map(String.class::cast).toList();
		}

		public List<String> getBlacklist() {
			return this.table.getArrayOrEmpty("blacklist").toList().stream().map(String.class::cast).toList();
		}
	}

	public record OpenEditorsConfig(TomlTable table, String key) implements ContextConfig {
		public long getLimit() {
			return this.table.getLong("limit", () -> 10L);
		}
	}

	public record ImportsConfig(TomlTable table, String key) implements ContextConfig {
	}

	public record SuperConfig(TomlTable table, String key) implements ContextConfig {
	}

	public record StickyConfig(TomlTable table, String key) implements ContextConfig {
	}

	public record TypeConfig(TomlTable table, String key) implements ContextConfig {
	}

	public record FillInMiddleConfig(TomlTable table, String key) implements ContextConfig {
		public int getMaxPrefixLength() {
			return (int) this.table.getLong("prefix_length", () -> AiCoderPreferences.getMaxPrefixSize());
		}

		public int getMaxSuffixLength() {
			return (int) this.table.getLong("suffix_length", () -> AiCoderPreferences.getMaxSuffixSize());
		}
	}

	public record CustomConfig(TomlTable table, String key) implements ContextConfig {
	}

	public record ClipboardConfig(TomlTable table, String key) implements ContextConfig {
	}

	public record LastEditsConfig(TomlTable table, String key) implements ContextConfig {
		public long getLimit() {
			return this.table.getLong("limit", () -> 10L);
		}
	}

	public record EmptyConfig(TomlTable table, String key) implements ContextConfig {
	}

	public record BlacklistedConfig(TomlTable table, String key) implements ContextConfig {
	}

	public record ScopeConfig(TomlTable table, String key) implements ContextConfig {
	}

	public record UserConfig(TomlTable table, String key) implements ContextConfig {
		public List<CustomContextEntryData> getEntries() {
			return this.table.getArrayOrEmpty("entry").toList().stream().map(TomlTable.class::cast).map(it -> {
				final String key = it.getString("key", () -> it.getString("title"));
				final String title = it.getString("title", () -> it.getString("key"));
				final String content = it.getString("content");
				final String glob = it.getString("glob");
				return new CustomContextEntryData(key, List.of(), title, content, glob);
			}).toList();
		}
	}

	public record RootConfig(TomlTable table, String key) implements ContextConfig {
	}

	public record TypeMemberConfig(TomlTable table, String key) implements ContextConfig {
	}

	public record PackageConfig(TomlTable table, String key) implements ContextConfig {
	}

	public record AiRerankConfig(TomlTable table, String key) implements ContextConfig {
		public int getLimit() {
			return (int) this.table.getLong("limit", () -> 10L);
		}

		public List<String> getWhitelist() {
			return this.table.getArrayOrEmpty("whitelist").toList().stream().map(String.class::cast).toList();
		}

		public List<String> getBlacklist() {
			return this.table.getArrayOrEmpty("blacklist").toList().stream().map(String.class::cast).toList();
		}
	}

	public record CodeViewportMemoryConfig(TomlTable table, String key) implements ContextConfig {
		public long getMaxLines() {
			return this.table.getLong("max_lines", () -> 1000L);
		}
	}

}

package de.hetzge.eclipse.aicoder.context;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import de.hetzge.eclipse.aicoder.ContextPreferences;
import mjson.Json;

public class CustomContextEntry extends ContextEntry {
	public static final String PREFIX = "CUSTOM";

	private final List<CustomContextEntry> childContextEntries;
	private final UUID id;
	private final String title;
	private final String content;
	private final String glob;

	public CustomContextEntry(List<CustomContextEntry> childContextEntries, UUID id, String title, String content, String glob, Duration creationDuration) {
		super(childContextEntries, creationDuration);
		this.childContextEntries = childContextEntries;
		this.id = id;
		this.title = title;
		this.content = content;
		this.glob = glob;
	}

	@Override
	public String getContent(ContextContext context) {
		return String.format("%s\n%s\n", this.content, super.getContent(context));
	}

	@Override
	public ContextEntryKey getKey() {
		return new ContextEntryKey(PREFIX, this.id.toString());
	}

	@Override
	public String getLabel() {
		return this.title;
	}

	@Override
	public List<CustomContextEntry> getChildContextEntries() {
		return this.childContextEntries;
	}

	public UUID getId() {
		return this.id;
	}

	public String getTitle() {
		return this.title;
	}

	public String getGlob() {
		return this.glob;
	}

	public Json toJson() {
		return Json.object()
				.set("children", this.childContextEntries.stream().map(CustomContextEntry::toJson).toList())
				.set("id", this.id.toString())
				.set("title", this.title)
				.set("content", this.content)
				.set("glob", this.glob);
	}

	public static Optional<CustomContextEntry> create(ContextEntryKey key) {
		return ContextPreferences.getCustomContextEntries().stream().filter(it -> Objects.equals(it.getId().toString(), key.value())).findFirst();
	}

	public static CustomContextEntry createFromJson(Json json) {
		final long before = System.currentTimeMillis();
		final List<CustomContextEntry> childEntries = json.at("children").asJsonList().stream().map(CustomContextEntry::createFromJson).toList();
		final UUID id = UUID.fromString(json.at("id").asString());
		final String title = json.at("title").asString();
		final String content = json.at("content").asString();
		final String glob = json.at("glob").asString();
		return new CustomContextEntry(childEntries, id, title, content, glob, Duration.ofMillis(System.currentTimeMillis() - before));
	}
}
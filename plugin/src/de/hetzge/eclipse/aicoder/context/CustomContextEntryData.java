package de.hetzge.eclipse.aicoder.context;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import de.hetzge.eclipse.aicoder.preferences.ContextPreferences;
import mjson.Json;

public class CustomContextEntryData {

	private final UUID id;
	private final List<CustomContextEntryData> children;
	private final String title;
	private final String content;
	private final String glob;

	public CustomContextEntryData(UUID id, List<CustomContextEntryData> children, String title, String content, String glob) {
		this.id = id;
		this.children = children;
		this.title = title;
		this.content = content;
		this.glob = glob;
	}

	public List<CustomContextEntryData> getChildren() {
		return this.children;
	}

	public String getContent() {
		return this.content;
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

	public boolean matches(Path path) {
		if (this.glob == null) {
			return false;
		}
		return FileSystems.getDefault().getPathMatcher("glob:" + this.glob).matches(path);
	}

	public Json toJson() {
		return Json.object()
				.set("children", this.children.stream().map(CustomContextEntryData::toJson).toList())
				.set("id", this.id.toString())
				.set("title", this.title)
				.set("content", this.content)
				.set("glob", this.glob);
	}

	public static Optional<CustomContextEntry> create(ContextEntryKey key) {
		return ContextPreferences.getCustomContextEntryDatas().stream()
				.filter(it -> Objects.equals(it.getId().toString(), key.value()))
				.findFirst()
				.map(data -> new CustomContextEntry(data, true));
	}

	public static CustomContextEntryData createFromJson(Json json) {
		final List<CustomContextEntryData> children = json.at("children").asJsonList().stream().map(CustomContextEntryData::createFromJson).toList();
		final UUID id = UUID.fromString(json.at("id").asString());
		final String title = json.at("title").asString();
		final String content = json.at("content").asString();
		final String glob = json.at("glob").asString();
		return new CustomContextEntryData(id, children, title, content, glob);
	}
}
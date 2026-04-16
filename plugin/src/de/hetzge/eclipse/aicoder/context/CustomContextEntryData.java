package de.hetzge.eclipse.aicoder.context;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import de.hetzge.eclipse.aicoder.preferences.ContextPreferences;
import de.hetzge.eclipse.aicoder.util.Utils;
import mjson.Json;

public class CustomContextEntryData {

	private final String key;
	private final List<CustomContextEntryData> children;
	private final String title;
	private final String content;
	private final String glob;

	public CustomContextEntryData(String key, List<CustomContextEntryData> children, String title, String content, String glob) {
		this.key = key;
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

	public String getKey() {
		return this.key;
	}

	public String getTitle() {
		return this.title;
	}

	public String getGlob() {
		return this.glob;
	}

	public boolean matches(Path path) {
		return Utils.matches(this.glob, path);
	}

	public Json toJson() {
		return Json.object()
				.set("children", this.children.stream().map(CustomContextEntryData::toJson).toList())
				.set("key", this.key)
				.set("title", this.title)
				.set("content", this.content)
				.set("glob", this.glob);
	}

	public static Optional<CustomContextEntry> create(ContextEntryKey key) {
		return ContextPreferences.getCustomContextEntryDatas().stream()
				.filter(it -> Objects.equals(it.getKey(), key.value()))
				.findFirst()
				.map(data -> new CustomContextEntry(data, true));
	}

	public static CustomContextEntryData createFromJson(Json json) {
		final List<CustomContextEntryData> children = json.at("children").asJsonList().stream().map(CustomContextEntryData::createFromJson).toList();
		final String key = json.at("key", json.at("id")).asString(); // TODO id is deprecated
		final String title = json.at("title").asString();
		final String content = json.at("content").asString();
		final String glob = json.at("glob").asString();
		return new CustomContextEntryData(key, children, title, content, glob);
	}
}
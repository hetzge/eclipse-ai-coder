package de.hetzge.eclipse.aicoder.content;

import java.util.Objects;

import mjson.Json;

public record EditInstruction(
		String key,
		String title,
		String content) {

	public EditInstruction {
		Objects.requireNonNull(key, "'key' must not be null");
		Objects.requireNonNull(title, "'title' must not be null");
		Objects.requireNonNull(content, "'content' must not be null");
	}

	public boolean match(String query) {
		return this.key.toLowerCase().contains(query.toLowerCase())
				|| this.title.toLowerCase().contains(query.toLowerCase())
				|| this.content.toLowerCase().contains(query.toLowerCase());
	}

	public Json toJson() {
		return Json.object()
				.set("key", this.key)
				.set("title", this.title)
				.set("content", this.content);
	}

	public static EditInstruction fromJson(Json json) {
		return new EditInstruction(
				json.at("key").asString(),
				json.at("title").asString(),
				json.at("content").asString());
	}

}

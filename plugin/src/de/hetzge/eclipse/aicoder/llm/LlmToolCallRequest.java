package de.hetzge.eclipse.aicoder.llm;

import mjson.Json;

public record LlmToolCallRequest(String id, String type, String functionName, Json arguments) {

	public Json toJson() {
		return Json.object()
				.set("id", this.id)
				.set("type", this.type)
				.set("functionName", this.functionName)
				.set("arguments", this.arguments);
	}

	public static LlmToolCallRequest fromJson(Json json) {
		return new LlmToolCallRequest(
				json.at("id").asString(),
				json.at("type").asString(),
				json.at("functionName").asString(),
				json.at("arguments"));
	}
}

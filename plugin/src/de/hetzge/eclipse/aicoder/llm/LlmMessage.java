package de.hetzge.eclipse.aicoder.llm;

import java.util.List;

import mjson.Json;

public record LlmMessage(LlmRole role, String content, String toolCallId, List<LlmToolCallRequest> toolCallRequest) {

	public LlmMessage(LlmRole role, String content) {
		this(role, content, null, null);
	}

	public LlmMessage(LlmRole role, String content, String toolCallId) {
		this(role, content, toolCallId, null);
	}

	public LlmMessage(LlmRole role, String content, List<LlmToolCallRequest> toolCallRequest) {
		this(role, content, null, toolCallRequest);
	}

	public Json toJson() {
		return Json.object()
				.set("role", this.role.name())
				.set("content", this.content)
				.set("toolCallId", this.toolCallId)
				.set("toolCallRequest", this.toolCallRequest != null ? Json.array(this.toolCallRequest.stream().map(it -> it.toJson()).toList()) : null);
	}

	public static LlmMessage fromJson(Json json) {
		return new LlmMessage(
				LlmRole.valueOf(json.at("role").asString()),
				json.at("content").asString(),
				json.at("toolCallId").isNull() ? null : json.at("toolCallId").asString(),
				json.at("toolCallRequest").isNull() ? null : json.at("toolCallRequest").asJsonList().stream().map(it -> LlmToolCallRequest.fromJson(it)).toList());
	}
}

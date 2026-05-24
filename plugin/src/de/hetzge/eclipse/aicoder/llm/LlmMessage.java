package de.hetzge.eclipse.aicoder.llm;

import java.util.List;

import mjson.Json;

public record LlmMessage(LlmRole role, String reasoning, String content, String toolCallId, List<LlmToolCallRequest> toolCallRequest) {

	public LlmMessage(LlmRole role, String content) {
		this(role, "", content, null, List.of());
	}

	public LlmMessage(LlmRole role, String reasoning, String content) {
		this(role, reasoning, content, null, List.of());
	}

	public LlmMessage(LlmRole role, String reasoning, String content, String toolCallId) {
		this(role, reasoning, content, toolCallId, List.of());
	}

	public LlmMessage(LlmRole role, String reasoning, String content, List<LlmToolCallRequest> toolCallRequest) {
		this(role, reasoning, content, null, toolCallRequest);
	}

	public LlmMessage {
		if (role == null) {
			throw new IllegalArgumentException("role must not be null");
		}
		if (reasoning == null) {
			throw new IllegalArgumentException("reasoning must not be null");
		}
		if (content == null) {
			throw new IllegalArgumentException("content must not be null");
		}
		if (toolCallRequest == null) {
			throw new IllegalArgumentException("toolCallRequest must not be null");
		}
	}

	public Json toJson() {
		return Json.object()
				.set("role", this.role.name())
				.set("reasoning", this.reasoning)
				.set("content", this.content)
				.set("toolCallId", this.toolCallId)
				.set("toolCallRequest", this.toolCallRequest != null ? this.toolCallRequest.stream().map(it -> it.toJson()).toList() : null);
	}

	public static LlmMessage fromJson(Json json) {
		return new LlmMessage(
				LlmRole.valueOf(json.at("role").asString()),
				!json.has("reasoning") || json.at("reasoning").isNull() ? "" : json.at("reasoning").asString(),
				json.at("content").asString(),
				json.at("toolCallId").isNull() ? null : json.at("toolCallId").asString(),
				json.at("toolCallRequest").isNull() ? List.of() : json.at("toolCallRequest").asJsonList().stream().map(it -> LlmToolCallRequest.fromJson(it)).toList());
	}
}

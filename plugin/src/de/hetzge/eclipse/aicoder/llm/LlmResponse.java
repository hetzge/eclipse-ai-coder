package de.hetzge.eclipse.aicoder.llm;

import java.time.Duration;
import java.util.List;

import mjson.Json;

public final class LlmResponse {
	private final int httpStatus;
	private final LlmOption llmModelOption;
	private final Json originalResponse;
	private final Duration duration;

	public LlmResponse(
			int httpStatus,
			LlmOption llmModelOption,
			Json originalResponse,
			Duration duration) {
		this.httpStatus = httpStatus;
		this.llmModelOption = llmModelOption;
		this.originalResponse = originalResponse;
		this.duration = duration;
	}

	public LlmResponse(Json json) {
		this.httpStatus = json.at("httpStatus").asInteger();
		this.llmModelOption = LlmOption.fromJson(json.at("llmModelOption"));
		this.originalResponse = json.at("originalResponse");
		this.duration = Duration.ofMillis(json.at("duration").asLong());
	}

	public Json toJson() {
		return Json.object()
				.set("httpStatus", this.httpStatus)
				.set("llmModelOption", this.llmModelOption.toJson())
				.set("originalResponse", this.originalResponse)
				.set("duration", this.duration.toMillis());
	}

	public int getHttpStatus() {
		return this.httpStatus;
	}

	public boolean isSuccess() {
		return this.httpStatus >= 200 && this.httpStatus < 300;
	}

	public LlmOption getLlmModelOption() {
		return this.llmModelOption;
	}

	public Json getOriginalResponse() {
		return this.originalResponse;
	}

	public String getReasoning() {
		if (this.llmModelOption.provider() == LlmProvider.OLLAMA) {
			return this.originalResponse.has("thinking") && this.originalResponse.at("thinking").isString()
					? this.originalResponse.at("thinking").asString()
					: "";
		} else if (this.llmModelOption.provider() == LlmProvider.MISTRAL) {
			return this.getReasoningFromMessage(this.getMessage());
		} else if (this.llmModelOption.provider() == LlmProvider.OPENAI || this.llmModelOption.provider() == LlmProvider.OPENAI_2 || this.llmModelOption.provider() == LlmProvider.OPENAI_3) {
			return this.getOpenAiReasoning();
		} else if (this.llmModelOption.provider() == LlmProvider.OPENROUTER) {
			return this.getOpenAiReasoning();
		} else if (this.llmModelOption.provider() == LlmProvider.INCEPTIONLABS) {
			return this.getReasoningFromMessage(this.getMessage());
		} else {
			throw new IllegalStateException("Unknown provider: " + this.llmModelOption.provider());
		}
	}

	public String getContent() {
		if (this.llmModelOption.provider() == LlmProvider.OLLAMA) {
			return this.originalResponse.at("response").asString();
		} else if (this.llmModelOption.provider() == LlmProvider.MISTRAL) {
			return this.getContentFromMessage(this.getMessage());
		} else if (this.llmModelOption.provider() == LlmProvider.OPENAI || this.llmModelOption.provider() == LlmProvider.OPENAI_2 || this.llmModelOption.provider() == LlmProvider.OPENAI_3) {
			return this.getOpenAiContent();
		} else if (this.llmModelOption.provider() == LlmProvider.OPENROUTER) {
			return this.getOpenAiContent();
		} else if (this.llmModelOption.provider() == LlmProvider.INCEPTIONLABS) {
			return this.getContentFromMessage(this.getMessage());
		} else {
			throw new IllegalStateException("Unknown provider: " + this.llmModelOption.provider());
		}
	}

	public String getPlainResponse() {
		final String reasoning = this.getReasoning();
		final String responseBody = this.originalResponse.toString();
		return reasoning.isEmpty() ? responseBody : responseBody + "\n\n" + reasoning;
	}

	public List<LlmToolCallRequest> getToolCallRequests() {
		if (this.llmModelOption.provider() == LlmProvider.OLLAMA) {
			return this.originalResponse.has("tool_calls") && this.originalResponse.at("tool_calls").isArray()
					? this.originalResponse.at("tool_calls").asJsonList().stream().map(toolCallJson -> this.parseToolCall(toolCallJson)).toList()
					: List.of();
		} else if (this.llmModelOption.provider() == LlmProvider.MISTRAL) {
			return this.getToolCallsFromMessage(this.getMessage());
		} else if (this.llmModelOption.provider() == LlmProvider.OPENAI || this.llmModelOption.provider() == LlmProvider.OPENAI_2 || this.llmModelOption.provider() == LlmProvider.OPENAI_3) {
			return this.getOpenAiToolCalls();
		} else if (this.llmModelOption.provider() == LlmProvider.OPENROUTER) {
			return this.getOpenAiToolCalls();
		} else if (this.llmModelOption.provider() == LlmProvider.INCEPTIONLABS) {
			return this.getToolCallsFromMessage(this.getMessage());
		} else {
			throw new IllegalStateException("Unknown provider: " + this.llmModelOption.provider());
		}
	}

	public int getInputTokens() {
		if (this.llmModelOption.provider() == LlmProvider.OLLAMA) {
			return this.originalResponse.at("prompt_eval_count", 0).asInteger();
		} else if (this.llmModelOption.provider() == LlmProvider.MISTRAL) {
			return this.originalResponse.at("usage").at("prompt_tokens").asInteger();
		} else if (this.llmModelOption.provider() == LlmProvider.OPENAI || this.llmModelOption.provider() == LlmProvider.OPENAI_2 || this.llmModelOption.provider() == LlmProvider.OPENAI_3) {
			return this.isResponsesApi()
					? this.originalResponse.at("usage").at("input_tokens", 0).asInteger()
					: this.originalResponse.at("usage").at("prompt_tokens", 0).asInteger();
		} else if (this.llmModelOption.provider() == LlmProvider.OPENROUTER) {
			return this.originalResponse.at("usage").at("prompt_tokens", 0).asInteger();
		} else if (this.llmModelOption.provider() == LlmProvider.INCEPTIONLABS) {
			return this.originalResponse.at("usage").at("prompt_tokens").asInteger();
		} else {
			throw new IllegalStateException("Unknown provider: " + this.llmModelOption.provider());
		}
	}

	public int getOutputTokens() {
		if (this.llmModelOption.provider() == LlmProvider.OLLAMA) {
			return this.originalResponse.at("eval_count", 0).asInteger();
		} else if (this.llmModelOption.provider() == LlmProvider.MISTRAL) {
			return this.originalResponse.at("usage").at("completion_tokens").asInteger();
		} else if (this.llmModelOption.provider() == LlmProvider.OPENAI || this.llmModelOption.provider() == LlmProvider.OPENAI_2 || this.llmModelOption.provider() == LlmProvider.OPENAI_3) {
			return this.isResponsesApi()
					? this.originalResponse.at("usage").at("output_tokens", 0).asInteger()
					: this.originalResponse.at("usage").at("completion_tokens", 0).asInteger();
		} else if (this.llmModelOption.provider() == LlmProvider.OPENROUTER) {
			return this.originalResponse.at("usage").at("completion_tokens", 0).asInteger();
		} else if (this.llmModelOption.provider() == LlmProvider.INCEPTIONLABS) {
			return this.originalResponse.at("usage").at("completion_tokens").asInteger();
		} else {
			throw new IllegalStateException("Unknown provider: " + this.llmModelOption.provider());
		}
	}

	public int getReasoningTokens() {
		if (this.llmModelOption.provider() == LlmProvider.OLLAMA) {
			return 0;
		} else if (this.llmModelOption.provider() == LlmProvider.MISTRAL) {
			return 0;
		} else if (this.llmModelOption.provider() == LlmProvider.OPENAI || this.llmModelOption.provider() == LlmProvider.OPENAI_2 || this.llmModelOption.provider() == LlmProvider.OPENAI_3) {
			return this.originalResponse.at("usage").at("reasoning_tokens", 0).asInteger();
		} else if (this.llmModelOption.provider() == LlmProvider.OPENROUTER) {
			return this.originalResponse.at("usage").at("reasoning_tokens", 0).asInteger();
		} else if (this.llmModelOption.provider() == LlmProvider.INCEPTIONLABS) {
			return 0;
		} else {
			throw new IllegalStateException("Unknown provider: " + this.llmModelOption.provider());
		}
	}

	public int getCachedTokens() {
		if (this.llmModelOption.provider() == LlmProvider.OLLAMA) {
			return this.originalResponse.at("prompt_eval_cached_count", 0).asInteger();
		} else if (this.llmModelOption.provider() == LlmProvider.MISTRAL) {
			return 0;
		} else if (this.llmModelOption.provider() == LlmProvider.OPENAI || this.llmModelOption.provider() == LlmProvider.OPENAI_2 || this.llmModelOption.provider() == LlmProvider.OPENAI_3) {
			return this.originalResponse.at("usage").at("cached_tokens", 0).asInteger();
		} else if (this.llmModelOption.provider() == LlmProvider.OPENROUTER) {
			return this.originalResponse.at("usage").at("cached_tokens", 0).asInteger();
		} else if (this.llmModelOption.provider() == LlmProvider.INCEPTIONLABS) {
			return 0;
		} else {
			throw new IllegalStateException("Unknown provider: " + this.llmModelOption.provider());
		}
	}

	public Duration getDuration() {
		return this.duration;
	}

	private boolean isResponsesApi() {
		return this.originalResponse.has("output") && this.originalResponse.at("output").isArray();
	}

	private Json getMessage() {
		return this.originalResponse.has("choices")
				? this.originalResponse.at("choices").at(0).at("message")
				: null;
	}

	private String getReasoningFromMessage(Json messageJson) {
		if (messageJson == null || !messageJson.isObject()) {
			return "";
		}
		if (messageJson.has("reasoning") && messageJson.at("reasoning").isString()) {
			return messageJson.at("reasoning").asString();
		}
		if (messageJson.has("reasoning_content") && messageJson.at("reasoning_content").isString()) {
			return messageJson.at("reasoning_content").asString();
		}
		return "";
	}

	private String getOpenAiReasoning() {
		if (this.isResponsesApi()) {
			return this.originalResponse.at("output").asJsonList().stream()
					.filter(item -> item != null && item.has("type") && "reasoning".equals(item.at("type").asString()))
					.findFirst()
					.map(item -> {
						if (item.has("summary") && item.at("summary").isArray()) {
							return String.join("\n", item.at("summary").asJsonList().stream()
									.filter(summary -> summary != null && summary.has("type") && "summary_text".equals(summary.at("type").asString()) && summary.has("text"))
									.map(summary -> summary.at("text").asString())
									.toList());
						}
						return "";
					})
					.orElse("");
		}
		return this.getReasoningFromMessage(this.getMessage());
	}

	private String getContentFromMessage(Json messageJson) {
		if (messageJson == null || !messageJson.isObject() || !messageJson.at("content").isString()) {
			return "";
		}
		return messageJson.at("content").asString();
	}

	private String getOpenAiContent() {
		if (this.isResponsesApi()) {
			return this.getContentFromResponsesApi(this.originalResponse.at("output").asJsonList());
		}
		if (this.originalResponse.has("choices")) {
			final Json firstChoice = this.originalResponse.at("choices").at(0);
			if (firstChoice.has("text") && firstChoice.at("text").isString()) {
				return firstChoice.at("text").asString();
			}
			return this.getContentFromMessage(firstChoice.at("message"));
		}
		return "";
	}

	private String getContentFromResponsesApi(List<Json> outputs) {
		final String content = outputs.stream()
				.filter(item -> item != null && item.has("type") && "message".equals(item.at("type").asString()))
				.findFirst()
				.map(item -> item.has("content") ? item.at("content") : null)
				.filter(inner -> inner != null)
				.map(inner -> inner.asJsonList().stream()
						.filter(contentItem -> contentItem != null && contentItem.has("type") && "output_text".equals(contentItem.at("type").asString()))
						.findFirst()
						.map(contentItem -> contentItem.has("text") ? contentItem.at("text").asString() : null)
						.orElse(null))
				.orElseGet(() -> outputs.stream()
						.filter(item -> item != null && item.has("content"))
						.findFirst()
						.map(item -> item.at("content").asJsonList().stream()
								.filter(contentItem -> contentItem != null && contentItem.has("text"))
								.findFirst()
								.map(contentItem -> contentItem.at("text").asString())
								.orElse(null))
						.orElse(""));
		return content == null ? "" : content;
	}

	private List<LlmToolCallRequest> getToolCallsFromMessage(Json messageJson) {
		if (messageJson == null || !messageJson.isObject()) {
			return List.of();
		}
		final Json toolCallsJson = messageJson.at("tool_calls");
		return toolCallsJson != null && toolCallsJson.isArray()
				? toolCallsJson.asJsonList().stream().map(toolCallJson -> this.parseToolCall(toolCallJson)).toList()
				: List.of();
	}

	private List<LlmToolCallRequest> getOpenAiToolCalls() {
		if (this.isResponsesApi()) {
			return this.originalResponse.at("output").asJsonList().stream()
					.filter(item -> item != null && item.has("type") && "function_call".equals(item.at("type").asString()))
					.map(toolCallJson -> new LlmToolCallRequest(
							toolCallJson.at("call_id").asString(),
							"function",
							toolCallJson.at("name").asString(),
							toolCallJson.at("arguments").isString()
									? Json.read(toolCallJson.at("arguments").asString())
									: toolCallJson.at("arguments")))
					.toList();
		}
		return this.getToolCallsFromMessage(this.getMessage());
	}

	private LlmToolCallRequest parseToolCall(Json toolCallJson) {
		return new LlmToolCallRequest(
				toolCallJson.at("id").asString(),
				toolCallJson.at("type").asString(),
				toolCallJson.at("function").at("name").asString(),
				Json.read(toolCallJson.at("function").at("arguments").asString()));
	}
}
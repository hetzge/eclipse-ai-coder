package de.hetzge.eclipse.aicoder.llm;

import java.time.Duration;
import java.util.List;

import mjson.Json;

public final class LlmResponse {
	private final LlmOption llmModelOption;
	private final Json originalResponse;
	private final String reasoning;
	private final String content;
	private final String plainResponse;
	private final List<LlmToolCallRequest> toolCallRequests;
	private final int inputTokens;
	private final int outputTokens;
	private final int reasoningTokens;
	private final int cachedTokens;
	private final Duration duration;
	private final boolean error;

	public LlmResponse(
			LlmOption llmModelOption,
			Json originalResponse,
			String reasoning,
			String content,
			String plainResponse,
			List<LlmToolCallRequest> toolCallRequests,
			int inputTokens,
			int outputTokens,
			int reasoningTokens,
			int cachedTokens,
			Duration duration,
			boolean error) {
		this.llmModelOption = llmModelOption;
		this.originalResponse = originalResponse;
		this.reasoning = reasoning;
		this.content = content;
		this.plainResponse = plainResponse;
		this.toolCallRequests = toolCallRequests;
		this.inputTokens = inputTokens;
		this.outputTokens = outputTokens;
		this.reasoningTokens = reasoningTokens;
		this.cachedTokens = cachedTokens;
		this.duration = duration;
		this.error = error;
	}

	public LlmOption getLlmModelOption() {
		return this.llmModelOption;
	}

	public String getReasoning() {
		return this.reasoning;
	}

	public String getContent() {
		return this.content;
	}

	public String getPlainResponse() {
		return this.plainResponse;
	}

	public List<LlmToolCallRequest> getToolCallRequests() {
		return this.toolCallRequests;
	}

	public int getInputTokens() {
		return this.inputTokens;
	}

	public int getOutputTokens() {
		return this.outputTokens;
	}

	public int getReasoningTokens() {
		return this.reasoningTokens;
	}

	public int getCachedTokens() {
		return this.cachedTokens;
	}

	public Duration getDuration() {
		return this.duration;
	}

	public boolean isError() {
		return this.error;
	}
}
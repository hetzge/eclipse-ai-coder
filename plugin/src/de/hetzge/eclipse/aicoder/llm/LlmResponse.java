package de.hetzge.eclipse.aicoder.llm;

import java.time.Duration;

public class LlmResponse {
	private final LlmOption llmModelOption;
	private final String content;
	private final String plainResponse;
	private final int inputTokens;
	private final int outputTokens;
	private final Duration duration;

	public LlmResponse(LlmOption llmModelOption, String content, String plainResponse, int inputTokens, int outputTokens, Duration duration) {
		this.llmModelOption = llmModelOption;
		this.content = content;
		this.plainResponse = plainResponse;
		this.inputTokens = inputTokens;
		this.outputTokens = outputTokens;
		this.duration = duration;
	}

	public LlmOption getLlmModelOption() {
		return this.llmModelOption;
	}

	public String getContent() {
		return this.content;
	}

	public String getPlainResponse() {
		return this.plainResponse;
	}

	public int getInputTokens() {
		return this.inputTokens;
	}

	public int getOutputTokens() {
		return this.outputTokens;
	}

	public Duration getDuration() {
		return this.duration;
	}
}
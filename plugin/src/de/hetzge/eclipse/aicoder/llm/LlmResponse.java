package de.hetzge.eclipse.aicoder.llm;

public class LlmResponse {
	private final String content;
	private final String plainResponse;
	private final int inputTokens;
	private final int outputTokens;

	public LlmResponse(String content, String plainResponse, int inputTokens, int outputTokens) {
		this.content = content;
		this.plainResponse = plainResponse;
		this.inputTokens = inputTokens;
		this.outputTokens = outputTokens;
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
}
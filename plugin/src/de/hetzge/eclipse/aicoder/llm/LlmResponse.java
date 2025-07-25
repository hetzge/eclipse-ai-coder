package de.hetzge.eclipse.aicoder.llm;

public class LlmResponse {
	private final String content;
	private final int inputTokens;
	private final int outputTokens;

	public LlmResponse(String content, int inputTokens, int outputTokens) {
		this.content = content;
		this.inputTokens = inputTokens;
		this.outputTokens = outputTokens;
	}

	public String getContent() {
		return this.content;
	}

	public int getInputTokens() {
		return this.inputTokens;
	}

	public int getOutputTokens() {
		return this.outputTokens;
	}
}
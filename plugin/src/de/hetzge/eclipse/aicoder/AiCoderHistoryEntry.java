package de.hetzge.eclipse.aicoder;

import java.time.LocalDateTime;

public class AiCoderHistoryEntry {
	private final LocalDateTime timestamp;
	private final String modelLabel;
	private final CompletionMode mode;
	private final String file;
	private String status;
	// Input stats
	private final String input;
	private final int inputCharacterCount;
	private final int inputWordCount;
	private final int inputLineCount;
	// Output stats
	private final String output;
	private final int outputCharacterCount;
	private final int outputWordCount;
	private final int outputLineCount;
	// Token counts
	private final int inputTokenCount;
	private final int outputTokenCount;
	private final long durationMs;
	private final long llmDurationMs;
	private final String plainLlmResponse;

	public AiCoderHistoryEntry(
			LocalDateTime timestamp,
			String modelLabel,
			CompletionMode mode,
			String file,
			String status,
			String input,
			int inputCharacterCount,
			int inputWordCount,
			int inputLineCount,
			String output,
			int outputCharacterCount,
			int outputWordCount,
			int outputLineCount,
			int inputTokenCount,
			int outputTokenCount,
			long durationMs,
			long llmDurationMs,
			String plainLlmResponse) {
		this.timestamp = timestamp;
		this.modelLabel = modelLabel;
		this.mode = mode;
		this.file = file;
		this.status = status;
		this.input = input;
		this.inputCharacterCount = inputCharacterCount;
		this.inputWordCount = inputWordCount;
		this.inputLineCount = inputLineCount;
		this.output = output;
		this.outputCharacterCount = outputCharacterCount;
		this.outputWordCount = outputWordCount;
		this.outputLineCount = outputLineCount;
		this.inputTokenCount = inputTokenCount;
		this.outputTokenCount = outputTokenCount;
		this.durationMs = durationMs;
		this.llmDurationMs = llmDurationMs;
		this.plainLlmResponse = plainLlmResponse;
	}

	public LocalDateTime getTimestamp() {
		return this.timestamp;
	}

	public String getModelLabel() {
		return this.modelLabel;
	}

	public CompletionMode getMode() {
		return this.mode;
	}

	public String getFile() {
		return this.file;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getStatus() {
		return this.status;
	}

	public String getInput() {
		return this.input;
	}

	public int getInputCharacterCount() {
		return this.inputCharacterCount;
	}

	public int getInputWordCount() {
		return this.inputWordCount;
	}

	public int getInputLineCount() {
		return this.inputLineCount;
	}

	public String getOutput() {
		return this.output;
	}

	public int getOutputCharacterCount() {
		return this.outputCharacterCount;
	}

	public int getOutputWordCount() {
		return this.outputWordCount;
	}

	public int getOutputLineCount() {
		return this.outputLineCount;
	}

	public int getInputTokenCount() {
		return this.inputTokenCount;
	}

	public int getOutputTokenCount() {
		return this.outputTokenCount;
	}

	public long getDurationMs() {
		return this.durationMs;
	}

	public String getFormattedDuration() {
		return formattedDuration(this.durationMs);
	}

	public String getFormattedLlmDuration() {
		return formattedDuration(this.llmDurationMs);
	}

	public String getPlainLlmResponse() {
		return this.plainLlmResponse;
	}

	private static String formattedDuration(long milliseconds) {
		if (milliseconds < 1000) {
			return milliseconds + "ms";
		} else {
			return String.format("%.1fs", milliseconds / 1000.0);
		}
	}

}
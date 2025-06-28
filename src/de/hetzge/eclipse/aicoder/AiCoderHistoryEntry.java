package de.hetzge.eclipse.aicoder;

import java.time.LocalDateTime;

public class AiCoderHistoryEntry {
	private final LocalDateTime timestamp;
	private final AiProvider provider;
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
	private final long durationMs;
	private final long llmDurationMs;

	public AiCoderHistoryEntry(
			LocalDateTime timestamp,
			AiProvider provider,
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
			long durationMs,
			long llmDurationMs) {
		this.timestamp = timestamp;
		this.provider = provider;
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
		this.durationMs = durationMs;
		this.llmDurationMs = llmDurationMs;
	}

	public LocalDateTime getTimestamp() {
		return this.timestamp;
	}

	public AiProvider getProvider() {
		return this.provider;
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

	public long getDurationMs() {
		return this.durationMs;
	}

	public String getFormattedDuration() {
		return formattedDuration(this.durationMs);
	}

	public String getFormattedLlmDuration() {
		return formattedDuration(this.llmDurationMs);
	}

	private static String formattedDuration(long milliseconds) {
		if (milliseconds < 1000) {
			return milliseconds + "ms";
		} else {
			return String.format("%.1fs", milliseconds / 1000.0);
		}
	}

}
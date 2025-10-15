package de.hetzge.eclipse.aicoder.history;

import java.time.LocalDateTime;

import de.hetzge.eclipse.aicoder.CompletionMode;

public class AiCoderHistoryEntry {
	private final LocalDateTime timestamp;
	private final String file;
	private final CompletionMode mode;
	private String modelLabel;
	private HistoryStatus status;
	// Input stats
	private String input;
	private int inputCharacterCount;
	private int inputWordCount;
	private int inputLineCount;
	// Output stats
	private String output;
	private int outputCharacterCount;
	private int outputWordCount;
	private int outputLineCount;
	// Token counts
	private int inputTokenCount;
	private int outputTokenCount;
	// Durations
	private long durationMs;
	private long llmDurationMs;
	private String plainLlmResponse;
	private String content;
	private final String previousContent;

	public AiCoderHistoryEntry(CompletionMode mode, String file, String previousContent) {
		this.mode = mode;
		this.timestamp = LocalDateTime.now();
		this.file = file;
		this.previousContent = previousContent;
		this.status = HistoryStatus.STARTED;
	}

	public CompletionMode getMode() {
		return this.mode;
	}

	public String getModelLabel() {
		return this.modelLabel;
	}

	public void setModelLabel(String modelLabel) {
		this.modelLabel = modelLabel;
	}

	public HistoryStatus getStatus() {
		return this.status;
	}

	public void setStatus(HistoryStatus status) {
		this.status = status;
	}

	public String getInput() {
		return this.input;
	}

	public void setInput(String input) {
		this.input = input;
		this.inputWordCount = input.split("\\s+").length;
		this.inputLineCount = (int) input.lines().count();
		this.inputCharacterCount = input.length();
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

	public void setOutput(String output) {
		this.output = output;
		this.outputWordCount = output.split("\\s+").length;
		this.outputLineCount = (int) output.lines().count();
		this.outputCharacterCount = output.length();
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

	public void setInputTokenCount(int inputTokenCount) {
		this.inputTokenCount = inputTokenCount;
	}

	public int getOutputTokenCount() {
		return this.outputTokenCount;
	}

	public void setOutputTokenCount(int outputTokenCount) {
		this.outputTokenCount = outputTokenCount;
	}

	public long getDurationMs() {
		return this.durationMs;
	}

	public void setDurationMs(long durationMs) {
		this.durationMs = durationMs;
	}

	public long getLlmDurationMs() {
		return this.llmDurationMs;
	}

	public void setLlmDurationMs(long llmDurationMs) {
		this.llmDurationMs = llmDurationMs;
	}

	public String getPlainLlmResponse() {
		return this.plainLlmResponse;
	}

	public void setPlainLlmResponse(String plainLlmResponse) {
		this.plainLlmResponse = plainLlmResponse;
	}

	public String getContent() {
		return this.content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getPreviousContent() {
		return this.previousContent;
	}

	public LocalDateTime getTimestamp() {
		return this.timestamp;
	}

	public String getFile() {
		return this.file;
	}

	public String getFormattedDuration() {
		return formattedDuration(this.durationMs);
	}

	public String getFormattedLlmDuration() {
		return formattedDuration(this.llmDurationMs);
	}

	public int getTokensPerSecond() {
		return (int) (this.outputTokenCount / (this.llmDurationMs / 1000.0));
	}

	private static String formattedDuration(long milliseconds) {
		if (milliseconds < 1000) {
			return milliseconds + "ms";
		} else {
			return String.format("%.1fs", milliseconds / 1000.0);
		}
	}
}
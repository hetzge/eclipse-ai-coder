package de.hetzge.eclipse.aicoder.inline;

import de.hetzge.eclipse.aicoder.history.AiCoderHistoryEntry;

public record Suggestion(
		AiCoderHistoryEntry historyEntry,
		String content,
		int modelOffset,
		int originalLength,
		int widgetLastLine,
		int newLines,
		int oldLines,
		int additionalLines) {

	public String applyTo(final String content) {
		final String prefix = content.substring(0, this.modelOffset);
		final String suffix = content.substring(this.modelOffset + this.originalLength);
		return prefix + this.content + suffix;
	}
}

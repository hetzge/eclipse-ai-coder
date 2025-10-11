package de.hetzge.eclipse.aicoder.inline;

public record Suggestion(
		String content,
		int modelOffset,
		int originalLength,
		int widgetLastLine,
		int newLines,
		int oldLines,
		int additionalLines) {
}

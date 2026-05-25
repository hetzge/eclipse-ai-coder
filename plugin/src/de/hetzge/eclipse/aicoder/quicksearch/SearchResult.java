package de.hetzge.eclipse.aicoder.quicksearch;

import org.eclipse.core.runtime.IPath;

/**
 * Represents a single search match result.
 */
public final class SearchResult {
	private final IPath path;
	private final int lineNumber;
	private final int columnStart;
	private final int columnEnd;
	private final String lineContent;
	private final String matchedText;

	public SearchResult(IPath path, int lineNumber, int columnStart, int columnEnd, String lineContent, String matchedText) {
		this.path = path;
		this.lineNumber = lineNumber;
		this.columnStart = columnStart;
		this.columnEnd = columnEnd;
		this.lineContent = lineContent;
		this.matchedText = matchedText;
	}

	public IPath getPath() {
		return this.path;
	}

	public int getLineNumber() {
		return this.lineNumber;
	}

	public int getColumnStart() {
		return this.columnStart;
	}

	public int getColumnEnd() {
		return this.columnEnd;
	}

	public String getLineContent() {
		return this.lineContent;
	}

	public String getMatchedText() {
		return this.matchedText;
	}

	@Override
	public String toString() {
		return String.format("%s:%d:%d - %s", this.path.toOSString(), this.lineNumber, this.columnStart, this.lineContent.trim());
	}
}
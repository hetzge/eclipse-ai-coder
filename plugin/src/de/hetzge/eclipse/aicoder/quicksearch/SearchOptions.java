package de.hetzge.eclipse.aicoder.quicksearch;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Configuration options for project search.
 */
public final class SearchOptions {
	private final Pattern pattern;
	private final boolean caseSensitive;
	private final boolean wholeWord;
	private final boolean searchBinaryFiles;
	private final Set<String> includedExtensions; // null = all extensions
	private final Set<String> excludedExtensions;
	private final int maxResults;
	private final int maxFileSizeBytes;

	private SearchOptions(Builder builder) {
		this.pattern = builder.pattern;
		this.caseSensitive = builder.caseSensitive;
		this.wholeWord = builder.wholeWord;
		this.searchBinaryFiles = builder.searchBinaryFiles;
		this.includedExtensions = builder.includedExtensions;
		this.excludedExtensions = builder.excludedExtensions;
		this.maxResults = builder.maxResults;
		this.maxFileSizeBytes = builder.maxFileSizeBytes;
	}

	public Pattern getPattern() {
		return this.pattern;
	}

	public boolean isCaseSensitive() {
		return this.caseSensitive;
	}

	public boolean isWholeWord() {
		return this.wholeWord;
	}

	public boolean searchBinaryFiles() {
		return this.searchBinaryFiles;
	}

	public Set<String> getIncludedExtensions() {
		return this.includedExtensions;
	}

	public Set<String> getExcludedExtensions() {
		return this.excludedExtensions;
	}

	public int getMaxResults() {
		return this.maxResults;
	}

	public int getMaxFileSizeBytes() {
		return this.maxFileSizeBytes;
	}

	public static Builder builder(String regex) {
		return new Builder(regex);
	}

	public static class Builder {
		private Pattern pattern;
		private boolean caseSensitive = true;
		private boolean wholeWord = false;
		private boolean searchBinaryFiles = false;
		private Set<String> includedExtensions = null;
		private Set<String> excludedExtensions = Set.of();
		private int maxResults = Integer.MAX_VALUE;
		private int maxFileSizeBytes = 10 * 1024 * 1024; // 10MB default

		public Builder(String regex) {
			this.pattern = Pattern.compile(regex);
		}

		public Builder caseSensitive(boolean caseSensitive) {
			if (!caseSensitive) {
				this.pattern = Pattern.compile(this.pattern.pattern(), this.pattern.flags() | Pattern.CASE_INSENSITIVE);
			}
			this.caseSensitive = caseSensitive;
			return this;
		}

		public Builder wholeWord(boolean wholeWord) {
			if (wholeWord) {
				this.pattern = Pattern.compile("\\b" + this.pattern.pattern() + "\\b",
						this.pattern.flags());
			}
			this.wholeWord = wholeWord;
			return this;
		}

		public Builder searchBinaryFiles(boolean search) {
			this.searchBinaryFiles = search;
			return this;
		}

		public Builder includedExtensions(Set<String> extensions) {
			this.includedExtensions = extensions;
			return this;
		}

		public Builder excludedExtensions(Set<String> extensions) {
			this.excludedExtensions = extensions;
			return this;
		}

		public Builder maxResults(int max) {
			this.maxResults = max;
			return this;
		}

		public Builder maxFileSizeBytes(int max) {
			this.maxFileSizeBytes = max;
			return this;
		}

		public SearchOptions build() {
			return new SearchOptions(this);
		}
	}
}
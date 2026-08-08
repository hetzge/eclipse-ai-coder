package de.hetzge.eclipse.aicoder.history;

import de.hetzge.eclipse.aicoder.CompletionMode;

public enum HistoryType {
	INLINE, EDIT, QUICK_FIX, GENERATE, NEXT_EDIT, RERANK, AGENT;

	public static HistoryType fromCompletionMode(CompletionMode mode) {
		switch (mode) {
		case INLINE:
			return INLINE;
		case EDIT:
			return EDIT;
		case QUICK_FIX:
			return QUICK_FIX;
		case GENERATE:
			return GENERATE;
		case NEXT_EDIT:
			return NEXT_EDIT;
		case AGENT:
			return AGENT;
		default:
			throw new IllegalArgumentException("Unexpected value: " + mode);
		}
	}
}

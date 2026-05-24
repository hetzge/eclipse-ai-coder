package de.hetzge.eclipse.aicoder;

import org.eclipse.jface.text.ITextViewer;

import de.hetzge.eclipse.aicoder.util.EclipseUtils;

public enum CompletionMode {
	INLINE, EDIT, QUICK_FIX, GENERATE, NEXT_EDIT, AGENT;

	public static CompletionMode getMode(ITextViewer textViewer, String instruction, boolean useAgent) {
		if (EclipseUtils.hasSelection(textViewer)) {
			if (instruction == null) {
				return CompletionMode.QUICK_FIX;
			} else if (useAgent) {
				return CompletionMode.AGENT;
			} else {
				return CompletionMode.EDIT;
			}
		} else {
			if (instruction == null) {
				return CompletionMode.INLINE;
			} else if (useAgent) {
				return CompletionMode.AGENT;
			} else {
				return CompletionMode.GENERATE;
			}
		}
	}
}

package de.hetzge.eclipse.aicoder.next;

import java.util.List;

import org.eclipse.core.runtime.IPath;

public record NextEditRequest(
		IPath currentLocation,
		String prefix,
		String editable,
		String suffix,
		List<LastLocation> locations,
		List<String> diffs) {

	public String toInceptionLabsNextEditPrompt() {
		final StringBuilder prompt = new StringBuilder();
		// Recently Viewed Snippets
		prompt.append("<|recently_viewed_code_snippets|>\n");
		if (this.locations != null && !this.locations.isEmpty()) {
			for (final LastLocation location : this.locations) {
				prompt.append("<|recently_viewed_code_snippet|>\n");
				prompt.append("code_snippet_file_path: ").append(location.path().toOSString()).append("\n");
				prompt.append(location.content()).append("\n");
				prompt.append("<|/recently_viewed_code_snippet|>\n\n");
			}
		}
		prompt.append("<|/recently_viewed_code_snippets|>\n\n");
		// Current File Content
		prompt.append("<|current_file_content|>\n");
		prompt.append("current_file_path: ").append(this.currentLocation.toOSString()).append("\n");
		prompt.append(this.prefix).append("\n");
		prompt.append("<|code_to_edit|>\n");
		prompt.append(this.editable).append("\n");
		prompt.append("<|/code_to_edit|>\n");
		prompt.append(this.suffix).append("\n");
		prompt.append("<|/current_file_content|>\n\n");
		// Edit History
		prompt.append("<|edit_diff_history|>\n");
		if (this.diffs != null && !this.diffs.isEmpty()) {
			for (final String diff : this.diffs) {
				prompt.append(diff).append("\n\n");
			}
		}
		prompt.append("<|/edit_diff_history|>");
		return prompt.toString();
	}
}

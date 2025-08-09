package de.hetzge.eclipse.aicoder.inline;

import java.util.Objects;

public record CodeCommand(
		int selectionOffset,
		String selectionText,
		String instruction) {

	public CodeCommand {
		Objects.requireNonNull(selectionText, "'selectionText' must not be null");
		Objects.requireNonNull(instruction, "'instruction' must not be null");
	}

	public boolean isInline() {
		return !this.selectionText.isBlank();
	}
}

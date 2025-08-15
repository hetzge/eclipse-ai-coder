package de.hetzge.eclipse.aicoder.mcp;

import java.util.List;

import de.hetzge.eclipse.aicoder.content.EditInstruction;

public record AiCoderMcpContent(
		String key,
		boolean success,
		String output,
		String title,
		String instructions,
		List<EditInstruction> editInstructions) {
}

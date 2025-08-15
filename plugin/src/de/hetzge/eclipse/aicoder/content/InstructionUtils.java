package de.hetzge.eclipse.aicoder.content;

import java.util.List;
import java.util.stream.Stream;

import de.hetzge.eclipse.aicoder.mcp.McpClients;

public final class InstructionUtils {

	private InstructionUtils() {
	}

	public static List<EditInstruction> getAllEditInstructions() {
		return Stream.concat(
				McpClients.INSTANCE.getEditInstructions().stream(),
				List.of(
						new EditInstruction("Complete", "Fix/complete the code", "Fix/complete the code"),
						new EditInstruction("Modernize", "Modernize the code", "Modernize the code"),
						new EditInstruction("Explain", "Document each step", "Document each step of the code with a meaningful comment. Provide interesting insights and hints."),
						new EditInstruction("Names", "Use better variable names", "Use better variable names"),
						new EditInstruction("For-loop", "Convert to for loop", "Convert to for loop"),
						new EditInstruction("Stream", "Convert to stream", "Convert to stream"),
						new EditInstruction("Monads", "Use monadic abstraction", "Make the code more readable by using monadic code (map, flatMap...) if possible")).stream())
				.toList();
	}

}

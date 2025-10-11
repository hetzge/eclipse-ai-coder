package de.hetzge.eclipse.aicoder.content;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import org.tomlj.Toml;
import org.tomlj.TomlParseResult;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.mcp.McpClients;

public final class InstructionUtils {

	private InstructionUtils() {
	}

	public static List<EditInstruction> getDefaultEditInstructions() {
		return List.of(
				new EditInstruction("Complete", "Fix/complete the code", "Fix/complete the code"),
				new EditInstruction("Modernize", "Modernize the code", "Modernize the code"),
				new EditInstruction("Explain", "Document each step", "Document each step of the code with a meaningful comment. Provide interesting insights and hints."),
				new EditInstruction("Names", "Use better variable names", "Use better variable names"),
				new EditInstruction("For-loop", "Convert to for loop", "Convert to for loop"),
				new EditInstruction("Stream", "Convert to stream", "Convert to stream"),
				new EditInstruction("Monads", "Use monadic abstraction", "Make the code more readable by using monadic code (map, flatMap...) if possible"),
				new EditInstruction("TODO-Driven-Development", "Implement TODOs", "Implement the TODOs. Remove the TODO comments."));
	}

	public static List<EditInstruction> resolve(Path file) {
		final List<EditInstruction> allInstructions = Stream.of(
				// Current directory
				file != null
						? Stream.iterate(file.getParent(), Path::getParent)
								.takeWhile(parentFolder -> parentFolder != null)
								.map(parentFolder -> parentFolder.resolve(".aicoder/instructions"))
								.filter(Files::exists)
								.filter(folder -> !Objects.equals(Path.of(System.getProperty("user.home"), ".aicoder/instructions"), folder))
								.flatMap(folder -> loadFromFolder(folder).stream())
								.toList().reversed().stream()
						: Stream.<EditInstruction>empty(),
				// User home directory
				Stream.of(Path.of(System.getProperty("user.home"), ".aicoder/instructions"))
						.filter(Files::exists)
						.flatMap(folder -> loadFromFolder(folder).stream()),
				// MCP server
				McpClients.INSTANCE.getEditInstructions().stream(),
				// Defaults
				getDefaultEditInstructions().stream(),
				// History
				AiCoderActivator.getDefault().getInstructionStorage().getEditInstructions().stream())
				.flatMap(Function.identity())
				.toList();
		return distinctByKeyAndSorted(allInstructions);
	}

	private static List<EditInstruction> distinctByKeyAndSorted(final List<EditInstruction> instructions) {
		final Map<String, EditInstruction> instructionMap = new HashMap<>();
		for (final EditInstruction instruction : instructions) {
			instructionMap.putIfAbsent(instruction.key(), instruction);
		}
		return instructionMap.values().stream()
				.sorted(Comparator.comparing(EditInstruction::key))
				.toList();
	}

	private static List<EditInstruction> loadFromFolder(Path folder) {
		try {
			return Files.walk(folder)
					.filter(path -> path.getFileName().toString().endsWith(".toml"))
					.flatMap(path -> loadFromFile(path).stream())
					.toList();
		} catch (final IOException exception) {
			throw new RuntimeException("Failed to load instructions from folder: " + folder, exception);
		}
	}

	private static Optional<EditInstruction> loadFromFile(Path file) {
		AiCoderActivator.log().info("Loading instruction from file: " + file);
		if (!file.getFileName().toString().endsWith(".toml")) {
			throw new IllegalArgumentException(String.format("File %s must end with '.toml'", file.getFileName()));
		}
		final String fileName = file.getFileName().toString();
		final String key = fileName.substring(0, fileName.length() - ".toml".length());
		TomlParseResult result;
		try {
			result = Toml.parse(file);
		} catch (final IOException exception) {
			AiCoderActivator.log().error("Failed to load instruction from file: " + file, exception);
			return Optional.empty();
		}

		final String title = result.getString("title") != null ? result.getString("title") : key;
		final String content = result.getString("content");
		if (content == null) {
			return Optional.empty();
		}
		return Optional.of(new EditInstruction(key, title, content.trim()));
	}
}

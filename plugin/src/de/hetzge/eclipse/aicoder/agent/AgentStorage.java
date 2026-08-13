package de.hetzge.eclipse.aicoder.agent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;

import com.github.f4b6a3.uuid.util.UuidComparator;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.llm.LlmRole;
import de.hetzge.eclipse.aicoder.trajectory.MessageTrajectoryEntry;
import de.hetzge.eclipse.aicoder.trajectory.TrajectoryEntry;
import de.hetzge.eclipse.aicoder.util.LambdaExceptionUtils;
import mjson.Json;

public final class AgentStorage {

	public static void saveAgentTask(AgentTask agentTask) throws IOException {
		final IPath taskPath = getTaskPath(agentTask.getId());
		Files.createDirectories(taskPath.toPath().getParent());
		Files.writeString(taskPath.toPath(), agentTask.toJson().toString(), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
	}

	public static Optional<AgentTask> loadAgentTask(UUID id) throws IOException {
		final IPath taskFilePath = getTaskPath(id);
		if (!Files.exists(taskFilePath.toPath())) {
			return Optional.empty();
		}
		final IWorkspace workspace = ResourcesPlugin.getWorkspace();
		return Optional.of(AgentTask.fromJson(workspace, Json.read(Files.readString(taskFilePath.toPath(), StandardCharsets.UTF_8))));
	}

	public static List<AgentTask> loadAgentTasks() throws IOException {
		Files.createDirectories(getTasksPath().toPath());
		return Files.list(getTasksPath().toPath())
				.map(it -> it.getFileName().toString())
				.filter(it -> it.endsWith(".json"))
				.map(it -> it.substring(0, it.length() - ".json".length()))
				.map(UUID::fromString)
				.sorted(UuidComparator.getDefaultInstance().reversed())
				.map(LambdaExceptionUtils.rethrowFunction(AgentStorage::loadAgentTask))
				.filter(Optional::isPresent)
				.map(Optional::get)
				.toList();
	}

	public static void deleteAgentTask(UUID id) throws IOException {
		Files.deleteIfExists(getTaskPath(id).toPath());
		Files.deleteIfExists(getTrajectoryFilePath(id).toPath());
		final IPath fileSystemPath = getFileSystemPath(id);
		final Path fileSystemFolder = fileSystemPath.toPath();
		if (Files.exists(fileSystemFolder)) {
			try (final Stream<Path> paths = Files.walk(fileSystemFolder)) {
				for (final Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
					Files.delete(path);
				}
			}
		}
	}

	public static void appendTrajectory(UUID id, TrajectoryEntry entry) throws IOException {
		final IPath trajectoryFilePath = getTrajectoryFilePath(id);
		Files.createDirectories(trajectoryFilePath.toPath().getParent());
		Files.writeString(trajectoryFilePath.toPath(),
				entry.toJson().toString() + "\n",
				StandardCharsets.UTF_8,
				StandardOpenOption.CREATE,
				StandardOpenOption.APPEND);
	}

	public static List<TrajectoryEntry> loadTrajectory(UUID id) throws IOException {
		final IPath trajectoryFilePath = getTrajectoryFilePath(id);
		if (!Files.exists(trajectoryFilePath.toPath())) {
			return List.of();
		}
		return Files.readAllLines(trajectoryFilePath.toPath(), StandardCharsets.UTF_8)
				.stream()
				.map(it -> TrajectoryEntry.fromJson(Json.read(it)))
				.filter(Optional::isPresent)
				.map(Optional::get)
				.toList();
	}

	/**
	 * Efficiently loads only the content of the last assistant message from the trajectory file.
	 * This avoids parsing the entire trajectory just to find the result message.
	 */
	public static Optional<String> loadLastAssistantMessageContent(UUID id) throws IOException {
		final IPath trajectoryFilePath = getTrajectoryFilePath(id);
		if (!Files.exists(trajectoryFilePath.toPath())) {
			return Optional.empty();
		}
		final List<String> lines = Files.readAllLines(trajectoryFilePath.toPath(), StandardCharsets.UTF_8);
		for (int i = lines.size() - 1; i >= 0; i--) {
			final String line = lines.get(i);
			if (line.isBlank()) {
				continue;
			}
			final Json json = Json.read(line);
			final Optional<MessageTrajectoryEntry> entry = MessageTrajectoryEntry.fromJson(json);
			if (entry.isPresent() && entry.get().message().role() == LlmRole.ASSISTANT) {
				return Optional.of(entry.get().message().content());
			}
		}
		return Optional.empty();
	}

	private static IPath getTrajectoryFilePath(final UUID id) {
		return getTrajectoriesPath().append(id.toString() + ".trajectory");
	}

	private static IPath getTrajectoriesPath() {
		return getStateLocationPath().append("trajectories");
	}

	private static IPath getTaskPath(UUID id) {
		return getTasksPath().append(id.toString() + ".json");
	}

	private static IPath getTasksPath() {
		return getStateLocationPath().append("tasks");
	}

	private static IPath getStateLocationPath() {
		return Platform.getStateLocation(AiCoderActivator.getDefault().getBundle());
	}

	public static IPath getFileSystemPath(UUID id) {
		return getStateLocationPath().append("filesystem").append(id.toString());
	}
}

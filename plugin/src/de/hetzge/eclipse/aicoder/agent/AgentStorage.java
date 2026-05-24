package de.hetzge.eclipse.aicoder.agent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;

import com.github.f4b6a3.uuid.util.UuidComparator;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.llm.LlmMessage;
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

	public static void appendTrajectory(UUID id, LlmMessage message) throws IOException {
		final IPath trajectoryFilePath = getTrajectoryFilePath(id);
		Files.createDirectories(trajectoryFilePath.toPath().getParent());
		Files.writeString(trajectoryFilePath.toPath(),
				message.toJson().toString() + "\n",
				StandardCharsets.UTF_8,
				StandardOpenOption.CREATE,
				StandardOpenOption.APPEND);
	}

	public static List<LlmMessage> loadTrajectory(UUID id) throws IOException {
		final IPath trajectoryFilePath = getTrajectoryFilePath(id);
		if (!Files.exists(trajectoryFilePath.toPath())) {
			return List.of();
		}
		return Files.readAllLines(trajectoryFilePath.toPath(), StandardCharsets.UTF_8)
				.stream()
				.map(it -> LlmMessage.fromJson(Json.read(it)))
				.toList();
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
}

package de.hetzge.eclipse.aicoder.agent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;

import com.github.f4b6a3.uuid.UuidCreator;
import com.github.f4b6a3.uuid.util.UuidComparator;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.agent.AgentRequest.ProjectNotFoundException;
import de.hetzge.eclipse.aicoder.llm.LlmMessage;
import de.hetzge.eclipse.aicoder.util.LambdaExceptionUtils;
import mjson.Json;

public final class AgentStorage {


	public static List<AgentTask> loadAgentTasks() throws IOException {
		return Files.list(getTrajectoryPath().toPath())
				.map(it -> it.getFileName().toString())
				.filter(it -> it.endsWith(".trajectory"))
				.map(it -> it.substring(0, it.length() - ".trajectory".length()))
				.map(UUID::fromString)
				.sorted(UuidComparator.getDefaultInstance())
				.map(LambdaExceptionUtils.rethrowFunction(AgentStorage::loadAgentTask))
				.filter(Optional::isPresent)
				.map(Optional::get)
				.toList();
	}

	public static void startTrajectory(AgentRequest request) throws IOException {
		final UUID id = UuidCreator.getTimeOrderedEpoch();
		final IPath trajectoryFilePath = getTrajectoryFilePath(id);
		Files.createDirectory(trajectoryFilePath.toPath().getParent());
		Files.writeString(trajectoryFilePath.toPath(),
				request.toJson().toString() + "\n",
				StandardCharsets.UTF_8,
				StandardOpenOption.CREATE_NEW);
	}

	public static void appendTrajectory(UUID id, LlmMessage message) throws IOException {
		final IPath trajectoryFilePath = getTrajectoryFilePath(id);
		Files.writeString(trajectoryFilePath.toPath(),
				message.toJson().toString() + "\n",
				StandardCharsets.UTF_8,
				StandardOpenOption.APPEND);
	}

	public static Optional<AgentTask> loadAgentTask(UUID id) throws IOException {
		final IPath trajectoryFilePath = getTrajectoryFilePath(id);
		if (!Files.exists(trajectoryFilePath.toPath())) {
			return Optional.empty();
		}
		final IWorkspace workspace = ResourcesPlugin.getWorkspace();
		try (Stream<String> lines = Files.lines(trajectoryFilePath.toPath(), StandardCharsets.UTF_8)) {
			final String firstLine = lines.findFirst().orElseThrow();
			try {
				return Optional.of(AgentTask.fromJson(workspace, Json.read(firstLine)));
			} catch (final ProjectNotFoundException exception) {
				AiCoderActivator.log().warn("Failed to load agent task for trajectory " + id + ": " + exception.getMessage());
				return Optional.empty();
			}
		}
	}

	public static List<LlmMessage> loadTrajectory(UUID id) throws IOException {
		final IPath trajectoryFilePath = getTrajectoryFilePath(id);
		if (!Files.exists(trajectoryFilePath.toPath())) {
			return List.of();
		}
		return Files.readAllLines(trajectoryFilePath.toPath(), StandardCharsets.UTF_8)
				.stream()
				.skip(1)
				.map(it -> LlmMessage.fromJson(Json.read(it)))
				.toList();
	}

	private static IPath getTrajectoryFilePath(final UUID id) {
		return getTrajectoryPath().append(id.toString() + ".trajectory");
	}

	private static IPath getTrajectoryPath() {
		return Platform.getStateLocation(AiCoderActivator.getDefault().getBundle()).append("trajectories");
	}
}

package de.hetzge.eclipse.aicoder.agent;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.github.f4b6a3.uuid.UuidCreator;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.llm.LlmMessage;
import de.hetzge.eclipse.aicoder.llm.LlmRole;
import de.hetzge.eclipse.aicoder.util.JinjaUtils;

public final class AgentService {

	private final Map<UUID, CompletableFuture<List<LlmMessage>>> trajectoryById;

	public AgentService() {
		this.trajectoryById = new ConcurrentHashMap<>();
	}

	public void execute(AgentRequest request) throws IOException {
		AiCoderActivator.log().info("Execute agent task");
		final String title = request.instructions().substring(0, Math.min(100, request.instructions().length()));
		final AgentTask task = new AgentTask(UuidCreator.getTimeOrderedEpoch(), title, request);
		AiCoderActivator.getDefault().getAgentTasksState().saveAgentTask(task);
		final List<LlmMessage> initialMessages = List.of(
				// TODO prompt preferences
				new LlmMessage(LlmRole.SYSTEM, """
						You are an AI coding assistant that implements tasks.
						You operate in an Eclipse IDE workspace. You have access to the following projects: ${PROJECTS}
						First understand the task. Then plan the implementation.
						Use the available tools to implement the task.
						""".replace("${PROJECTS}", request.projects().stream().map(it -> it.getName()).collect(Collectors.joining(", ")))),
				new LlmMessage(LlmRole.USER, JinjaUtils.applyTemplate("""
						Implement this task:
						{{ task }}

						{% if selection_content -%}
						The user has selected the following code in the file {{ selection_file_path }}:
						```{{ selection_file_extension }}
						{{ selection_content }}
						```
						{% else -%}
						The user is currently in the file {{ selection_file_path }}.
						{% endif -%}
						""", Map.ofEntries(
						Map.entry("task", request.instructions()),
						Map.entry("selection_file_path", request.selection().path().toString()),
						Map.entry("selection_file_extension", request.selection().path().getFileExtension()),
						Map.entry("selection_content", request.selection().content())))));
		for (final LlmMessage message : initialMessages) {
			AiCoderActivator.getDefault().getAgentTasksState().appendTrajectory(task.getId(), message);
		}
		final CompletableFuture<List<LlmMessage>> future = AgentLoop.execute(request.projects(), initialMessages, message -> {
			try {
				AiCoderActivator.getDefault().getAgentTasksState().appendTrajectory(task.getId(), message);
			} catch (final Exception exception) {
				throw new RuntimeException("Failed to append trajectory", exception);
			}
		}).whenComplete((result, exception) -> {
			try {
				this.trajectoryById.remove(task.getId());
				if (exception != null) {
					AiCoderActivator.log().error("Failed to execute agent task", exception);
					task.setStatus(AgentStatus.ERROR);
					AgentStorage.saveAgentTask(task);
				}
				if (result != null) {
					AiCoderActivator.log().info("Agent task completed with " + result.size() + " messages");
					task.setStatus(AgentStatus.SUCCESS);
					AgentStorage.saveAgentTask(task);
				}
			} catch (final IOException ioException) {
				AiCoderActivator.log().error("Failed to save agent task", ioException);
				throw new RuntimeException("Failed to save agent task", ioException);
			}
		});
		this.trajectoryById.put(task.getId(), future);
	}

	public void cancel(UUID id) {
		final CompletableFuture<List<LlmMessage>> future = this.trajectoryById.get(id);
		if (future != null) {
			future.cancel(true);
		}
	}
}

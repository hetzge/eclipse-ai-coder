package de.hetzge.eclipse.aicoder.agent;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IProject;

import com.github.f4b6a3.uuid.UuidCreator;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.llm.LlmMessage;
import de.hetzge.eclipse.aicoder.llm.LlmRole;
import de.hetzge.eclipse.aicoder.tool.EditFileTool;
import de.hetzge.eclipse.aicoder.tool.FileSystem;
import de.hetzge.eclipse.aicoder.tool.ListFilesTool;
import de.hetzge.eclipse.aicoder.tool.ReadFileTool;
import de.hetzge.eclipse.aicoder.tool.SearchTool;
import de.hetzge.eclipse.aicoder.tool.Tool;
import de.hetzge.eclipse.aicoder.util.JinjaUtils;

public final class AgentService {

	private final Map<UUID, AgentTaskExecution> executionsById;

	public AgentService() {
		this.executionsById = new ConcurrentHashMap<>();
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
		final List<IProject> projects = request.projects();
		final FileSystem fileSystem = new FileSystem(projects, projects.get(0).getWorkspace().getRoot());
		final List<Tool> tools = List.of(
				new EditFileTool(projects, fileSystem),
				new ListFilesTool(projects, fileSystem),
				new ReadFileTool(projects, fileSystem),
				new SearchTool(projects, fileSystem));
		final AtomicBoolean cancelled = new AtomicBoolean(false);
		final CompletableFuture<List<LlmMessage>> loopFuture = AgentLoop.execute(tools, projects, initialMessages, message -> {
			try {
				AiCoderActivator.getDefault().getAgentTasksState().appendTrajectory(task.getId(), message);
				task.setChanges(fileSystem.toAgentChanges());
				AgentStorage.saveAgentTask(task);
				fileSystem.persist(AgentStorage.getFileSystemPath(task.getId()).toPath());
				AiCoderActivator.getDefault().getAgentTasksState().fireAgentTasksChanged(task);

			} catch (final Exception exception) {
				throw new RuntimeException("Failed to append trajectory", exception);
			}
		}, cancelled);
		loopFuture.whenComplete((result, exception) -> {
			try {
				this.executionsById.remove(task.getId());
				if (exception instanceof final CancellationException cancellationException) {
					AiCoderActivator.log().info("Agent task aborted");
					task.setStatus(AgentStatus.CANCELLED);
					AgentStorage.saveAgentTask(task);
				} else if (exception != null) {
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
			} finally {
				if (task.getStatus() == AgentStatus.RUNNING) {
					task.setStatus(AgentStatus.ERROR);
					try {
						AgentStorage.saveAgentTask(task);
					} catch (final IOException innerException) {
						AiCoderActivator.log().error("Failed to save agent task", innerException);
					}
				}
			}
			AiCoderActivator.getDefault().getAgentTasksState().fireAgentTasksChanged(task);
		});
		this.executionsById.put(task.getId(), new AgentTaskExecution(loopFuture, cancelled));
	}

	public void abort(UUID id) {
		final AgentTaskExecution execution = this.executionsById.get(id);
		if (execution != null) {
			execution.cancelled.set(true);
			execution.future.cancel(true);
		}
	}

	public void abortAll() {
		for (final UUID id : List.copyOf(this.executionsById.keySet())) {
			abort(id);
		}
	}

	public void cancel(UUID id) {
		abort(id);
	}

	private static final class AgentTaskExecution {
		private final CompletableFuture<List<LlmMessage>> future;
		private final AtomicBoolean cancelled;

		private AgentTaskExecution(CompletableFuture<List<LlmMessage>> future, AtomicBoolean cancelled) {
			this.future = future;
			this.cancelled = cancelled;
		}
	}
}

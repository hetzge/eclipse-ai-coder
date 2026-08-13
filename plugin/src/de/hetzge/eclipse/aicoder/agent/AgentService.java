package de.hetzge.eclipse.aicoder.agent;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.github.f4b6a3.uuid.UuidCreator;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.llm.LlmMessage;
import de.hetzge.eclipse.aicoder.llm.LlmRole;
import de.hetzge.eclipse.aicoder.trajectory.MessageTrajectoryEntry;
import de.hetzge.eclipse.aicoder.tool.CreateFileTool;
import de.hetzge.eclipse.aicoder.tool.EditFileTool;
import de.hetzge.eclipse.aicoder.tool.FileSystem;
import de.hetzge.eclipse.aicoder.tool.ListFilesTool;
import de.hetzge.eclipse.aicoder.tool.ReadFileTool;
import de.hetzge.eclipse.aicoder.tool.SearchTool;
import de.hetzge.eclipse.aicoder.tool.Tool;
import de.hetzge.eclipse.aicoder.util.JinjaUtils;

public final class AgentService {

	private final Map<UUID, AgentTaskJob> jobById;

	public AgentService() {
		this.jobById = new ConcurrentHashMap<>();
	}

	public void execute(AgentRequest request) throws IOException {
		AiCoderActivator.log().info("Execute agent task");
		final String title = request.instructions().substring(0, Math.min(50, request.instructions().length())).replaceAll("\\s+", " ");
		final AgentTaskJob job = new AgentTaskJob(title, request);
		job.schedule();
	}

	public void rerun(UUID id) {
		abort(id);
		final Optional<AgentTask> taskOptional = AiCoderActivator.getDefault().getAgentTasksState().findTask(id);
		if (!taskOptional.isPresent()) {
			AiCoderActivator.log().warn("Agent task not found: " + id);
			return;
		}
		final AgentTask task = taskOptional.get();
		try {
			execute(task.getRequest());
		} catch (final IOException exception) {
			AiCoderActivator.log().error("Failed to rerun agent task", exception);
			AiCoderActivator.openErrorDialog("Failed to rerun agent task", "Failed to rerun agent task", exception);
		}
	}

	public void abort(UUID id) {
		final AgentTaskJob job = this.jobById.get(id);
		if (job != null) {
			job.cancel();
		}
	}

	public void abortAll() {
		for (final UUID id : List.copyOf(this.jobById.keySet())) {
			abort(id);
		}
	}

	public void cancel(UUID id) {
		abort(id);
	}

	public void delete(UUID id) {
		final AgentTaskJob job = this.jobById.get(id);
		if (job != null) {
			job.delete();
		}
		try {
			AiCoderActivator.getDefault().getAgentTasksState().deleteAgentTask(id);
		} catch (final IOException exception) {
			AiCoderActivator.log().error("Failed to delete agent task", exception);
		}
	}

	private final class AgentTaskJob extends Job {

		private final String title;
		private final AgentRequest request;
		private volatile boolean deleted;

		public AgentTaskJob(String title, AgentRequest request) {
			super("Agent: " + title);
			this.title = title;
			this.request = request;
		}

		public void delete() {
			this.deleted = true;
			cancel();
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			final AgentTask task = new AgentTask(UuidCreator.getTimeOrderedEpoch(), this.title, this.request);
			try {
				try {
					AgentService.this.jobById.put(task.getId(), this);
					AiCoderActivator.getDefault().getAgentTasksState().saveAgentTask(task);
					final String fileExtension = this.request.selection().path().getFileExtension();
					final List<LlmMessage> initialMessages = List.of(
							// TODO prompt preferences
							new LlmMessage(LlmRole.SYSTEM, """
									You are an AI coding assistant.
									You operate in an Eclipse IDE workspace. You have access to the following projects: ${PROJECTS}
									Use the available tools to implement the request.
									""".replace("${PROJECTS}", this.request.projects().stream().map(it -> it.getName()).collect(Collectors.joining(", ")))),
							new LlmMessage(LlmRole.USER, JinjaUtils.applyTemplate("""
									User request:
									{{ task }}

									{% if selection_content -%}
									The user has selected the following code in the file "{{ selection_file_path }}" in the project "{{ project }}":
									```{{ selection_file_extension }}
									{{ selection_content }}
									```
									{% else -%}
									The user is currently in the file/folder "{{ selection_file_path }}" in the project "{{ project }}".
									{% endif -%}
									""", Map.ofEntries(
									Map.entry("task", this.request.instructions()),
									Map.entry("selection_file_path", this.request.selection().path().removeFirstSegments(1).makeRelative().toString()),
									Map.entry("project", this.request.projects().get(0).getName()),
									Map.entry("selection_file_extension", fileExtension != null ? fileExtension : ""),
									Map.entry("selection_content", this.request.selection().content())))));
					for (final LlmMessage message : initialMessages) {
						AiCoderActivator.getDefault().getAgentTasksState().appendTrajectory(task.getId(), new MessageTrajectoryEntry(message));
					}
					final List<IProject> projects = this.request.projects();
					final FileSystem fileSystem = new FileSystem(projects, projects.get(0).getWorkspace().getRoot());

					final List<Tool> tools;
					if (this.request.readonly()) {
						tools = List.of(
								new ListFilesTool(projects, fileSystem),
								new ReadFileTool(projects, fileSystem),
								new SearchTool(projects, fileSystem));
					} else {
						tools = List.of(
								new CreateFileTool(projects, fileSystem),
								new EditFileTool(projects, fileSystem),
								new ListFilesTool(projects, fileSystem),
								new ReadFileTool(projects, fileSystem),
								new SearchTool(projects, fileSystem)
						// TODO disabled until fixed
						// new ProblemsTool(projects),
						// new BuildTool(projects, fileSystem)
						);
					}
					final List<LlmMessage> result = AgentLoop.execute(monitor, this.request.llmOption(), tools, projects, initialMessages, message -> {
						if (!AgentTaskJob.this.deleted) {
							try {
								AiCoderActivator.getDefault().getAgentTasksState().appendTrajectory(task.getId(), new MessageTrajectoryEntry(message));
								task.setChanges(fileSystem.toAgentChanges());
								AgentStorage.saveAgentTask(task);
								fileSystem.persist(AgentStorage.getFileSystemPath(task.getId()).toPath());
								AiCoderActivator.getDefault().getAgentTasksState().fireAgentTasksChanged(task);
							} catch (final Exception exception) {
								throw new RuntimeException("Failed to append trajectory", exception);
							}
						}
					});
					AiCoderActivator.log().info("Agent task completed with " + result.size() + " messages");
					if (!AgentTaskJob.this.deleted) {
						task.setStatus(AgentStatus.SUCCESS);
						AgentStorage.saveAgentTask(task);
					}
				} catch (final CancellationException exception) {
					AiCoderActivator.log().info("Agent task aborted");
					if (!AgentTaskJob.this.deleted) {
						task.setStatus(AgentStatus.CANCELLED);
						AgentStorage.saveAgentTask(task);
					}
				} catch (final Exception exception) {
					AiCoderActivator.log().error("Failed to execute agent task", exception);
					if (!AgentTaskJob.this.deleted) {
						task.setStatus(AgentStatus.ERROR);
						AgentStorage.saveAgentTask(task);
					}
				} finally {
					AgentService.this.jobById.remove(task.getId());
					if (!AgentTaskJob.this.deleted) {
						if (task.getStatus() == AgentStatus.RUNNING) {
							task.setStatus(AgentStatus.ERROR);
							try {
								AgentStorage.saveAgentTask(task);
							} catch (final IOException innerException) {
								AiCoderActivator.log().error("Failed to save agent task", innerException);
							}
						}
						AiCoderActivator.getDefault().getAgentTasksState().fireAgentTasksChanged(task);
					}
				}
				return Status.OK_STATUS;
			} catch (final IOException exception) {
				throw new RuntimeException("Failed to run agent job", exception);
			}
		}
	}
}

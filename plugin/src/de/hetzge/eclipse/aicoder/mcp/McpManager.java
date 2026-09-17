package de.hetzge.eclipse.aicoder.mcp;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.content.EditInstruction;
import de.hetzge.eclipse.aicoder.preferences.AiCoderPreferences;
import de.hetzge.eclipse.aicoder.util.Utils;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema.Content;
import io.modelcontextprotocol.spec.McpSchema.GetPromptRequest;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;

public final class McpManager implements AutoCloseable {

	private final Map<String, McpInstance> serverByName;

	public McpManager() {
		this.serverByName = new ConcurrentHashMap<>();
	}

	@Override
	public void close() {
		for (final McpInstance instance : this.serverByName.values()) {
			try {
				instance.client().close();
			} catch (final Exception exception) {
				AiCoderActivator.log().error("Failed to close MCP client for server: " + instance.name(), exception);
			}
		}
	}

	public List<EditInstruction> getEditInstructions() {
		return this.serverByName.values().stream()
				.flatMap(instance -> instance.editInstructions().stream())
				.toList();
	}

	public List<McpInstance> getMcpServers() {
		return this.serverByName.values().stream().toList();
	}

	public Optional<McpInstance> getMcpServer(String name) {
		return Optional.ofNullable(this.serverByName.get(name));
	}

	public void startAllMcpServers(Runnable onComplete) {
		startAllMcpServers(AiCoderPreferences.getMcpServerConfigurations().asJsonMap().entrySet().stream()
				.collect(ConcurrentHashMap::new, (map, entry) -> map.put(entry.getKey(), McpConfiguration.fromJson(entry.getValue())), ConcurrentHashMap::putAll), onComplete);
	}

	public void startAllMcpServers(Map<String, McpConfiguration> configurations, Runnable onComplete) {
		final Job job = new Job("Start MCP servers") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				final List<Entry<String, McpConfiguration>> entries = configurations.entrySet().stream().toList();
				final List<? extends Job> jobs = entries.stream().map(entry -> {
					return new Job("Start MCP server: " + entry.getKey()) {
						@Override
						protected IStatus run(IProgressMonitor innerMonitor) {
							McpManager.this.serverByName.put(entry.getKey(), startMcpServer(entry.getKey(), entry.getValue()));
							return Status.OK_STATUS;
						}
					};
				}).toList();
				for (final Job job : jobs) {
					job.schedule();
				}
				for (int i = 0; i < jobs.size(); i++) {
					final Job job = jobs.get(i);
					final Entry<String, McpConfiguration> entry = entries.get(i);
					try {
						job.join(Duration.ofSeconds(60).toMillis(), monitor);
					} catch (OperationCanceledException | InterruptedException exception) {
						AiCoderActivator.log().warn(String.format("Timeout while start MCP server: %s", entry.getKey()));
						McpManager.this.serverByName.put(entry.getKey(), new McpInstance(entry.getKey(), null, new StringBuilder().append(Utils.stacktraceToString(exception)), List.of(), List.of()));
					}
				}
				onComplete.run();
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}

	public McpInstance startMcpServer(String name, McpConfiguration configuration) {
		AiCoderActivator.log().info("Starting MCP server: " + name);
		stopMcpServer(name);
		final StringBuilder logBuilder = new StringBuilder();
		final McpSyncClient client = McpClient.sync(configuration.toMcpTransport())
				.loggingConsumer(notification -> {
					AiCoderActivator.log().info("MCP Notification: " + notification);
					logBuilder.append(notification).append("\n");
				})
				.progressConsumer(progress -> {
					AiCoderActivator.log().info("Progress: " + progress.progress() + "/" + progress.total());
				})
				.build();
		client.initialize();
		final List<EditInstruction> editInstructions = client.listPrompts().prompts().stream()
				.filter(prompt -> {
					// only prompts without arguments are supported
					return prompt.arguments().isEmpty();
				})
				.flatMap(prompt -> {
					final List<PromptMessage> messages = client.getPrompt(new GetPromptRequest(prompt.name(), Map.of())).messages();
					final Content content = messages.getFirst().content();
					if (!(content instanceof TextContent)) {
						// only text prompts are supported
						return Stream.empty();
					}
					final TextContent textContent = (TextContent) content;
					final String text = textContent.text();
					final String firstLine = text.lines().findFirst().orElse("");
					final String title = prompt.title() != null ? prompt.title() : firstLine.substring(0, Math.min(100, firstLine.length()));
					return Stream.of(new EditInstruction("MCP/%s/%s".formatted(name, prompt.name()), title, text));
				})
				.toList();
		final List<Tool> tools = client.listTools().tools();
		AiCoderActivator.log().info("MCP prompts in " + name + ": " + editInstructions.size());
		AiCoderActivator.log().info("MCP tools in " + name + ": " + tools.size());
		return new McpInstance(name, client, logBuilder, editInstructions, tools);
	}

	public void stopMcpServer(String name) {
		final McpInstance instance = this.serverByName.remove(name);
		if (instance != null) {
			AiCoderActivator.log().info("Stopping MCP server: " + name);
			instance.client().close();
		}
	}
}

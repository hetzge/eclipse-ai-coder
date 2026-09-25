package de.hetzge.eclipse.aicoder.agent;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.CompletionMode;
import de.hetzge.eclipse.aicoder.history.HistoryEntry;
import de.hetzge.eclipse.aicoder.history.HistoryStatus;
import de.hetzge.eclipse.aicoder.llm.LlmMessage;
import de.hetzge.eclipse.aicoder.llm.LlmOption;
import de.hetzge.eclipse.aicoder.llm.LlmRequest;
import de.hetzge.eclipse.aicoder.llm.LlmResponse;
import de.hetzge.eclipse.aicoder.llm.LlmRole;
import de.hetzge.eclipse.aicoder.llm.LlmToolCallRequest;
import de.hetzge.eclipse.aicoder.llm.LlmToolDefinition;
import de.hetzge.eclipse.aicoder.llm.LlmUtils;
import de.hetzge.eclipse.aicoder.preferences.AiCoderPreferences;
import de.hetzge.eclipse.aicoder.tool.Tool;

// TODO pause loop or recontinue loop
// TODO retry on 429
// TODO highlight active task lines in editor (store line range in agent task)
// TODO token counts
// TODO focused agentic edit mode (tool to provide only replacement for selected text)
// TODO fix arguments json with library
// TODO edited files as children of the task tree

public final class AgentLoop {

	private AgentLoop() {
	}

	public static List<LlmMessage> execute(IProgressMonitor monitor, LlmOption llmModelOption, List<Tool> tools, List<IProject> projects, List<LlmMessage> initialMessages, Consumer<LlmMessage> messageConsumer) {
		if (projects.isEmpty()) {
			throw new IllegalArgumentException("At least one project must be provided.");
		}
		final long startTime = System.currentTimeMillis();
		final HistoryEntry historyEntry = new HistoryEntry(
				UUID.randomUUID(),
				CompletionMode.AGENT,
				projects.get(0).getFullPath().toPath(),
				buildContext(initialMessages),
				"",
				List.of(),
				Duration.ZERO,
				HistoryStatus.STARTED);
		historyEntry.persist();
		final List<LlmToolDefinition> toolDefinitions = tools
				.stream()
				.map(it -> new LlmToolDefinition(it.getDefinition()))
				.toList();
		final List<LlmMessage> messages = new ArrayList<>(initialMessages);
		final int maxIterations = AiCoderPreferences.getMaxAgentIterations();
		final int toolCallOutputLimit = AiCoderPreferences.getToolCallOutputLimit();
		int iteration = 0;
		try {
			while (true) {
				iteration++;
				if (iteration > maxIterations) {
					throw new IllegalStateException("Max agent iterations reached: " + maxIterations);
				}
				checkCancelled(monitor);
				AiCoderActivator.log().info("Agent loop iteration with model: " + llmModelOption.modelKey() + " and " + messages.size() + " messages");
				final LlmResponse response = LlmUtils.executeAgent(llmModelOption, new LlmRequest(messages, toolDefinitions)).join();
				checkCancelled(monitor);
				updateHistoryEntry(historyEntry, response, startTime);
				final LlmMessage assistantMessage = new LlmMessage(LlmRole.ASSISTANT, response.getReasoning(), response.getContent(), response.getToolCallRequests());
				messages.add(assistantMessage);
				messageConsumer.accept(assistantMessage);
				final List<LlmToolCallRequest> toolCallRequests = response.getToolCallRequests();
				for (final LlmToolCallRequest toolCallRequest : toolCallRequests) {
					checkCancelled(monitor);
					AiCoderActivator.log().info("Executing tool '" + toolCallRequest.functionName() + "' with arguments " + toolCallRequest.arguments());
					final Optional<Tool> toolOptional = findTool(tools, toolCallRequest);
					if (toolOptional.isEmpty()) {
						throw new IllegalStateException("Unknown tool: " + toolCallRequest.functionName()); // TODO
					}
					final Tool tool = toolOptional.get();
					final String toolResponse = truncateToolResponse(tool.execute(monitor, toolCallRequest.arguments()), toolCallOutputLimit);
					final LlmMessage toolResponseMessage = new LlmMessage(LlmRole.TOOL, "", toolResponse, toolCallRequest.id(), List.of());
					messages.add(toolResponseMessage);
					messageConsumer.accept(toolResponseMessage);
				}
				if (toolCallRequests.isEmpty()) {
					historyEntry.update(sink -> {
						sink.setStatus(HistoryStatus.GENERATED);
						sink.setDuration(Duration.ofMillis(System.currentTimeMillis() - startTime));
					});
					return messages;
				}
			}
		} catch (final CancellationException exception) {
			historyEntry.update(sink -> {
				sink.setStatus(HistoryStatus.CANCELED);
				sink.setDuration(Duration.ofMillis(System.currentTimeMillis() - startTime));
			});
			throw exception;
		} catch (final RuntimeException exception) {
			historyEntry.update(sink -> {
				sink.setStatus(HistoryStatus.ERROR);
				sink.setDuration(Duration.ofMillis(System.currentTimeMillis() - startTime));
			});
			throw exception;
		}
	}

	private static String buildContext(List<LlmMessage> initialMessages) {
		final StringBuilder builder = new StringBuilder();
		for (final LlmMessage message : initialMessages) {
			if (!builder.isEmpty()) {
				builder.append("\n\n");
			}
			builder.append(message.role().name()).append(": ").append(message.content());
		}
		return builder.toString();
	}

	private static void updateHistoryEntry(HistoryEntry historyEntry, LlmResponse response, long startTime) {
		historyEntry.update(sink -> {
			sink.addResponse(response);
			sink.setContent(response.getContent());
			sink.setDuration(Duration.ofMillis(System.currentTimeMillis() - startTime));
		});
	}

	private static String truncateToolResponse(String response, int limit) {
		if (response.length() <= limit) {
			return response;
		}
		final String truncatedMessage = "\n...[Tool output truncated because the limit of " + limit + " characters was reached]";
		if (limit <= truncatedMessage.length()) {
			return response.substring(0, limit);
		}
		return response.substring(0, limit - truncatedMessage.length()) + truncatedMessage;
	}

	private static void checkCancelled(IProgressMonitor monitor) {
		if (monitor.isCanceled()) {
			throw new CancellationException("Agent task aborted");
		}
	}

	private static Optional<Tool> findTool(final List<Tool> tools, final LlmToolCallRequest toolCallRequest) {
		return tools.stream().filter(it -> it.getName().equals(toolCallRequest.functionName())).findFirst();
	}
}

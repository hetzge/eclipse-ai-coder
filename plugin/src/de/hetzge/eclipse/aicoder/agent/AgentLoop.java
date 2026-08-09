package de.hetzge.eclipse.aicoder.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.preferences.AiCoderPreferences;
import de.hetzge.eclipse.aicoder.llm.LlmMessage;
import de.hetzge.eclipse.aicoder.llm.LlmOption;
import de.hetzge.eclipse.aicoder.llm.LlmRequest;
import de.hetzge.eclipse.aicoder.llm.LlmResponse;
import de.hetzge.eclipse.aicoder.llm.LlmRole;
import de.hetzge.eclipse.aicoder.llm.LlmToolCallRequest;
import de.hetzge.eclipse.aicoder.llm.LlmToolDefinition;
import de.hetzge.eclipse.aicoder.llm.LlmUtils;
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

	public static List<LlmMessage> execute(IProgressMonitor monitor, List<Tool> tools, List<IProject> projects, List<LlmMessage> initialMessages, Consumer<LlmMessage> messageConsumer) {
		if (projects.isEmpty()) {
			throw new IllegalArgumentException("At least one project must be provided.");
		}
		final List<LlmToolDefinition> toolDefinitions = tools
				.stream()
				.map(it -> new LlmToolDefinition(it.getDefinition()))
				.toList();
		final LlmOption llmModelOption = LlmOption.createEditModelOptionFromPreferences(); // TODO
		final List<LlmMessage> messages = new ArrayList<>(initialMessages);
		final int maxIterations = AiCoderPreferences.getMaxAgentIterations();
		final int toolCallOutputLimit = AiCoderPreferences.getToolCallOutputLimit();
		int iteration = 0;
		while (true) {
			iteration++;
			if (iteration > maxIterations) {
				throw new IllegalStateException("Max agent iterations reached: " + maxIterations);
			}
			checkCancelled(monitor);
			AiCoderActivator.log().info("Agent loop iteration with model: " + llmModelOption.modelKey() + " and " + messages.size() + " messages");
			final LlmResponse response = LlmUtils.executeAgent(llmModelOption, new LlmRequest(messages, toolDefinitions)).join();
			checkCancelled(monitor);
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
				final LlmMessage toolResponseMessage = new LlmMessage(LlmRole.TOOL, response.getReasoning(), toolResponse, toolCallRequest.id(), List.of());
				messages.add(toolResponseMessage);
				messageConsumer.accept(toolResponseMessage);
			}
			if (toolCallRequests.isEmpty()) {
				return messages;
			}
		}
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

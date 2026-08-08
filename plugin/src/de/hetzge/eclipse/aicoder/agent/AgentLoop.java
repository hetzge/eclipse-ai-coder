package de.hetzge.eclipse.aicoder.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.eclipse.core.resources.IProject;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.llm.LlmMessage;
import de.hetzge.eclipse.aicoder.llm.LlmOption;
import de.hetzge.eclipse.aicoder.llm.LlmRequest;
import de.hetzge.eclipse.aicoder.llm.LlmResponse;
import de.hetzge.eclipse.aicoder.llm.LlmRole;
import de.hetzge.eclipse.aicoder.llm.LlmToolCallRequest;
import de.hetzge.eclipse.aicoder.llm.LlmToolDefinition;
import de.hetzge.eclipse.aicoder.llm.LlmUtils;
import de.hetzge.eclipse.aicoder.tool.Tool;

// TODO limit loop
// TODO pause loop or recontinue loop
// TODO retry on 429
// TODO highlight active task lines in editor (store line range in agent task)
// TODO token counts
// TODO focused agentic edit mode (tool to provide only replacement for selected text)
// TODO fix arguments json with library
// TODO limit output length
// TODO edited files as children of the task tree

public final class AgentLoop {

	private AgentLoop() {
	}

	public static CompletableFuture<List<LlmMessage>> execute(List<Tool> tools, List<IProject> projects, List<LlmMessage> initialMessages, Consumer<LlmMessage> messageConsumer) {
		return execute(tools, projects, initialMessages, messageConsumer, new AtomicBoolean(false));
	}

	public static CompletableFuture<List<LlmMessage>> execute(List<Tool> tools, List<IProject> projects, List<LlmMessage> initialMessages, Consumer<LlmMessage> messageConsumer, AtomicBoolean cancelled) {
		if (projects.isEmpty()) {
			throw new IllegalArgumentException("At least one project must be provided.");
		}
		return CompletableFuture.supplyAsync(() -> {
			final List<LlmToolDefinition> toolDefinitions = tools
					.stream()
					.map(it -> new LlmToolDefinition(it.getDefinition()))
					.toList();
			final LlmOption llmModelOption = LlmOption.createEditModelOptionFromPreferences(); // TODO
			final List<LlmMessage> messages = new ArrayList<>(initialMessages);
			while (true) {
				checkCancelled(cancelled);
				AiCoderActivator.log().info("Agent loop iteration with model: " + llmModelOption.modelKey() + " and " + messages.size() + " messages");
				if (messages.size() > 100) { // TODO
					throw new IllegalStateException("Too many messages");
				}
				final LlmResponse response = LlmUtils.executeAgent(llmModelOption, new LlmRequest(messages, toolDefinitions)).join();
				checkCancelled(cancelled);
				final LlmMessage assistantMessage = new LlmMessage(LlmRole.ASSISTANT, response.getReasoning(), response.getContent(), response.getToolCallRequests());
				messages.add(assistantMessage);
				messageConsumer.accept(assistantMessage);
				final List<LlmToolCallRequest> toolCallRequests = response.getToolCallRequests();
				for (final LlmToolCallRequest toolCallRequest : toolCallRequests) {
					checkCancelled(cancelled);
					AiCoderActivator.log().info("Executing tool '" + toolCallRequest.functionName() + "' with arguments " + toolCallRequest.arguments());
					final Optional<Tool> toolOptional = findTool(tools, toolCallRequest);
					if (toolOptional.isEmpty()) {
						throw new IllegalStateException("Unknown tool: " + toolCallRequest.functionName()); // TODO
					}
					final Tool tool = toolOptional.get();
					final String toolResponse = tool.execute(toolCallRequest.arguments());
					final LlmMessage toolResponseMessage = new LlmMessage(LlmRole.TOOL, response.getReasoning(), toolResponse, toolCallRequest.id(), List.of());
					messages.add(toolResponseMessage);
					messageConsumer.accept(toolResponseMessage);
				}
				if (toolCallRequests.isEmpty()) {
					return messages;
				}
			}
		});
	}

	private static void checkCancelled(AtomicBoolean cancelled) {
		if (cancelled.get()) {
			throw new CancellationException("Agent task aborted");
		}
	}

	private static Optional<Tool> findTool(final List<Tool> tools, final LlmToolCallRequest toolCallRequest) {
		return tools.stream().filter(it -> it.getName().equals(toolCallRequest.functionName())).findFirst();
	}
}

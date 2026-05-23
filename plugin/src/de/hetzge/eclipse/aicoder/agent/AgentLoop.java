package de.hetzge.eclipse.aicoder.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import org.eclipse.core.resources.IProject;

import de.hetzge.eclipse.aicoder.llm.LlmMessage;
import de.hetzge.eclipse.aicoder.llm.LlmOption;
import de.hetzge.eclipse.aicoder.llm.LlmRequest;
import de.hetzge.eclipse.aicoder.llm.LlmResponse;
import de.hetzge.eclipse.aicoder.llm.LlmRole;
import de.hetzge.eclipse.aicoder.llm.LlmToolCallRequest;
import de.hetzge.eclipse.aicoder.llm.LlmToolDefinition;
import de.hetzge.eclipse.aicoder.llm.LlmUtils;
import de.hetzge.eclipse.aicoder.tool.EditFileTool;
import de.hetzge.eclipse.aicoder.tool.ListFilesTool;
import de.hetzge.eclipse.aicoder.tool.ReadFileTool;
import de.hetzge.eclipse.aicoder.tool.SearchTool;
import de.hetzge.eclipse.aicoder.tool.Tool;

// TODO limit loop
// TODO pause loop or recontinue loop
// TODO retry on 429
// TODO highlight active task lines in editor (store line range in agent task)

public final class AgentLoop {

	private AgentLoop() {
	}

	public static List<LlmMessage> execute(Executor executor, IProject project, String instructions, Consumer<LlmMessage> messageConsumer) {
		final List<LlmMessage> initialMessages = List.of(
				new LlmMessage(LlmRole.SYSTEM, "You are an AI assistant that helps to edit code in an Eclipse project."),
				new LlmMessage(LlmRole.USER, instructions));
		return execute(executor, project, initialMessages, messageConsumer);
	}

	public static List<LlmMessage> execute(Executor executor, IProject project, List<LlmMessage> initialMessages, Consumer<LlmMessage> messageConsumer) {
		final List<Tool> tools = List.of(
				new EditFileTool(project),
				new ListFilesTool(project),
				new ReadFileTool(project),
				new SearchTool(project));
		final List<LlmToolDefinition> toolDefinitions = tools
				.stream()
				.map(it -> new LlmToolDefinition(it.getDefinition()))
				.toList();
		final LlmOption llmModelOption = LlmOption.createEditModelOptionFromPreferences(); // TODO
		final List<LlmMessage> messages = new ArrayList<>(initialMessages);
		while (true) {
			if (messages.size() > 100) { // TODO
				throw new IllegalStateException("Too many messages");
			}
			final LlmResponse response = LlmUtils.executeAgent(llmModelOption, new LlmRequest(messages, toolDefinitions)).join();
			final LlmMessage assistantMessage = new LlmMessage(LlmRole.ASSISTANT, response.getContent(), response.getToolCallRequests());
			messages.add(assistantMessage);
			messageConsumer.accept(assistantMessage);
			final List<LlmToolCallRequest> toolCallRequests = response.getToolCallRequests();
			for (final LlmToolCallRequest toolCallRequest : toolCallRequests) {
				final Optional<Tool> toolOptional = findTool(tools, toolCallRequest);
				if (toolOptional.isEmpty()) {
					throw new IllegalStateException("Unknown tool: " + toolCallRequest.functionName()); // TODO
				}
				final Tool tool = toolOptional.get();
				final String toolResponse = tool.execute(toolCallRequest.arguments());
				final LlmMessage toolResponseMessage = new LlmMessage(LlmRole.TOOL, toolResponse, toolCallRequest.id(), null);
				messages.add(toolResponseMessage);
				messageConsumer.accept(toolResponseMessage);
			}
			if (toolCallRequests.isEmpty()) {
				return messages;
			}
		}
	}

	private static Optional<Tool> findTool(final List<Tool> tools, final LlmToolCallRequest toolCallRequest) {
		return tools.stream().filter(it -> it.getName().equals(toolCallRequest.functionName())).findFirst();
	}
}

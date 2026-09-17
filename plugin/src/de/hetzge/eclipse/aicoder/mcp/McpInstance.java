package de.hetzge.eclipse.aicoder.mcp;

import java.util.List;
import java.util.stream.Collectors;

import de.hetzge.eclipse.aicoder.content.EditInstruction;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import mjson.Json;

public final record McpInstance(String name, McpSyncClient client, StringBuilder logBuilder, List<EditInstruction> editInstructions, List<Tool> tools) {

	public boolean isInitialized() {
		return this.client != null && this.client.isInitialized();
	}

	public String getLogs() {
		return this.logBuilder.toString();
	}

	public String callTool(String toolName, Json arguments) {
		if (!this.isInitialized()) {
			throw new IllegalStateException("MCP client for server '" + this.name + "' is not initialized");
		}
		final CallToolResult result = this.client.callTool(new CallToolRequest(toolName, arguments.asMap()));
		return result.content().stream()
				.filter(TextContent.class::isInstance)
				.map(TextContent.class::cast)
				.map(it -> it.text())
				.collect(Collectors.joining("\n---\n"));
	}

	public int getToolCount() {
		return this.tools.size();
	}

	public int getPromptCount() {
		return this.editInstructions.size();
	}
}

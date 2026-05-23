package de.hetzge.eclipse.aicoder.llm;

import java.util.List;

public record LlmRequest(List<LlmMessage> messages, List<LlmToolDefinition> toolDefinitions, String prefix, String suffix) {

	public LlmRequest(List<LlmMessage> messages, List<LlmToolDefinition> toolDefinitions) {
		this(messages, toolDefinitions, null, null);
	}

}

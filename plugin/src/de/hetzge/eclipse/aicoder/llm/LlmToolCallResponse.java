package de.hetzge.eclipse.aicoder.llm;

public record LlmToolCallResponse(String id, String type, String functionName, String result) {

}

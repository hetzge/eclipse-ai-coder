package de.hetzge.eclipse.aicoder.llm;

public final class LlmPromptTemplates {
	private LlmPromptTemplates() {
	}

	public static String changeCodePrompt(String fileType, String code, String instructions) {
		return """
				You are a software developer assistant. Your task is to change the given code according to the given instructions.
				IMPORTANT:
				- Only provide the edited code.
				- Do not provide any explanations or comments.
				- Do not edit or mention code that is not provided to you.
				- Do not edit/add lines that are not part of the provided code.
				Here is the code to edit:
				```%s
				%s
				```
				Here are the instructions:
				%s
				""".trim().formatted(fileType, code, instructions);

	}
}

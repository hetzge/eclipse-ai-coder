package de.hetzge.eclipse.aicoder.llm;

public final class LlmPromptTemplates {
	private LlmPromptTemplates() {
	}

	public static String changeCodeSystemPrompt() {
		return """
				You are a software developer assistant. Your task is to change the given code according to the given instructions.
				IMPORTANT:
				- Only provide the edited code.
				- Do not provide any explanations or comments.
				- Do not edit or mention code that is not provided to you.
				- Do not edit/add lines that are not part of the provided code.
				""".trim();
	}

	public static String changeCodePrompt(String fileType, String code, String instructions, String prefix, String suffix) {
		return """
				Here is some context:
				%s<<<SELECTION LOCATION>>>%s

				# Here is the code to edit:
				```%s
				%s
				```
				# Here are the instructions:
				%s
				""".trim().formatted(prefix, suffix, fileType, code, instructions);

	}
}

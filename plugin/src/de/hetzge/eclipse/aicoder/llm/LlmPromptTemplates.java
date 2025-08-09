package de.hetzge.eclipse.aicoder.llm;

public final class LlmPromptTemplates {
	private LlmPromptTemplates() {
	}

	public static String changeCodeSystemPrompt() {
		return """
				You are a software developer assistant.
				Your task is to CHANGE the given code according to the given instructions.
				"<<<SELECTION LOCATION>>>" marks the location of the code that should be changed
				INFORMATIONS PROVIDED TO YOU:
				- Context: Sourcecode around the edit location, available types, guidelines, ...
				- Code to edit: The code you have to change
				- Edit instructions: How the code should be edited
				REQUIREMENTS/CONSTRAINTS:
				- Only provide the edited code
				- Do NOT provide any explanations or comments
				- Do NOT edit or mention code that is not provided to you
				- Do NOT edit/add lines that are not part of the provided code
				""".trim();
	}

	public static String generateCodeSystemPrompt() {
		return """
				You are a software developer assistant.
				Your task is to GENERATE code according to the given instructions.
				"<<<GENERATE LOCATION>>>" marks the location where the generated code will be placed in
				INFORMATIONS PROVIDED TO YOU:
				- Context: Sourcecode around the generate location, available types, guidelines, ...
				- Generate instructions: What code should be generated
				REQUIREMENTS/CONSTRAINTS:
				- Only provide the generated code
				- Do NOT provide any explanations or comments
				- Do NOT generate code that is already there
				""".trim();
	}

	public static String changeCodePrompt(String fileType, String code, String instructions, String prefix, String suffix) {
		return """
				CONTEXT:
				%s<<<SELECTION LOCATION>>>%s
				\n=============================\n
				CODE TO EDIT:
				```%s
				%s
				```
				\n=============================\n
				EDIT INSTRUCTIONS:
				%s
				""".trim().formatted(prefix, suffix, fileType, code, instructions);
	}

	public static String generateCodePrompt(String instructions, String prefix, String suffix) {
		return """
				CONTEXT:
				%s<<<GENERATE LOCATION>>>%s
				\n=============================\n
				GENERATE INSTRUCTIONS:
				%s
				"""
				.trim().formatted(prefix, suffix, instructions);
	}
}

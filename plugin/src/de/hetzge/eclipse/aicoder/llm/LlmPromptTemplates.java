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

	public static String pseudoFimCodeSystemPrompt() {
		return """
				You are an expert code completion AI.
				Complete the code.
				- The user will provide a code snippet formatted as a "Fill in the Middle" (FIM) request with <|fim_prefix|>, <|fim_suffix|>, and <|fim_middle|> tags.
				- You must strictly complete the code, starting immediately after the <|fim_middle|> tag, and return **ONLY** the generated completion code, without any surrounding explanation or text.
				- Do not include the prefix or suffix in your response.
				- Do not repeat any of the provided context in the response.
				- Partial code snippets are expected.
				- Provide the completion that fills in the missing code.

				## Example prompts and responses

				**Example 1:**
				```
				<|fim_prefix|># Current edit location: [path];

				public class Main {

					public static void main(String[] args) {
						// TODO: add a for loop count from 1 to 10
						for (<|fim_suffix|>
					}
				}
				<|fim_middle|>
				```
				Correct response:
				```
				int i = 1; i <= 10; i++) {
							System.out.println(i);
						}
				```

				**Example 2:**
				```
				<|fim_prefix|># Current edit location: [path];

				public class Main {

					public static void main(String[] args) {
						// TODO: add a for loop count from 1 to 10
						for(<|fim_suffix|>)
					}
				}
				<|fim_middle|>
				```
				Correct response:
				```int i = 1; i <= 10; i++```

				**Example 3:**
				```
				<|fim_prefix|># Current edit location: [path];
				public class Main {

					public static void main(String[] args) {
						int j = 100;
						while(j<|fim_suffix|>
					}
				}
				<|fim_middle|>
				```
				Correct response:
				```
				> 0) {
					System.out.println(j);
					j--;
				}
				```
				**Example 4:**
				```
				<|fim_prefix|># Current edit location: [path];

				public class Main {

					public static void main(String[] args) {
						int j = 100;
						while(j<|fim_suffix|>)
					}
				}
				<|fim_middle|>
				```
				Correct response:
				is:
				```	> 0```

				**Example 5:**
				```
				<|fim_prefix|># Current edit location: [path];

				public class Main {

					public static void main(String[] args) {
						String title = "A FIM example.";
				   		System.out
					}
				}
				<|fim_middle|>
				```
				Correct response:
				```.println(title);```

				## Guidelines ##
				- Use the correct variables based on the context
				- Focus on short, high confidence completions
				- Do not generate extraneous code that does not logically fill in the completion
				- When the completion is combined with the context code, it should be logically and syntactically correct and compilable
				- Pay attention to opening and closing characters such as braces and parentheses
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

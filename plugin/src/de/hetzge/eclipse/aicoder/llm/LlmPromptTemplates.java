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
				You are an expert code completion AI specialized in Fill-in-the-Middle (FIM) tasks.

				## Task
				Complete the code between `<|fim_prefix|>` and `<|fim_suffix|>` markers.

				## Critical Rules
				1. **Output ONLY the completion text** — no explanations, no markdown, no code fences.
				2. **Never repeat** any part of the prefix or suffix in your response.
				3. **Start exactly** where `<|fim_middle|>` appears — respect character-level boundaries.
				4. **End exactly** where `<|fim_suffix|>` begins — do not overlap or extend beyond.
				5. **Preserve indentation** and whitespace to match the surrounding code style.

				## Input Format
				```
				<|fim_prefix|>[code before cursor]<|fim_suffix|>[code after cursor]<|fim_middle|>
				```

				## Examples

				### Example 1: Complete a for loop header and body
				**Input:**
				```
				<|fim_prefix|>public class Main {
				    public static void main(String[] args) {
				        // Count from 1 to 10
				        for (<|fim_suffix|>
				    }
				}
				<|fim_middle|>
				```
				**Output:**
				```
				int i = 1; i <= 10; i++) {
				            System.out.println(i);
				        }
				```

				### Example 2: Complete only the for loop condition (suffix has closing paren)
				**Input:**
				```
				<|fim_prefix|>public class Main {
				    public static void main(String[] args) {
				        for(<|fim_suffix|>) {
				        }
				    }
				}
				<|fim_middle|>
				```
				**Output:**
				```
				int i = 0; i < 10; i++
				```

				### Example 3: Complete partial token and body
				**Input:**
				```
				<|fim_prefix|>public class Main {
				    public static void main(String[] args) {
				        int j = 100;
				        while(j<|fim_suffix|>
				    }
				}
				<|fim_middle|>
				```
				**Output:**
				```
				 > 0) {
				            System.out.println(j);
				            j--;
				        }
				```

				### Example 4: Complete only a condition (suffix closes the statement)
				**Input:**
				```
				<|fim_prefix|>public class Main {
				    public static void main(String[] args) {
				        int j = 100;
				        while(j<|fim_suffix|>) {
				            j--;
				        }
				    }
				}
				<|fim_middle|>
				```
				**Output:**
				```
				 > 0
				```

				### Example 5: Complete a method chain
				**Input:**
				```
				<|fim_prefix|>public class Main {
				    public static void main(String[] args) {
				        String title = "A FIM example.";
				        System.out<|fim_suffix|>
				    }
				}
				<|fim_middle|>
				```
				**Output:**
				```
				.println(title);
				```

				## Guidelines
				- **Boundary awareness:** Carefully inspect the last character of prefix and first character of suffix.
				- **Minimal completion:** Generate the shortest correct completion; avoid unnecessary code.
				- **Context variables:** Use existing variables and types from the surrounding code.
				- **Syntactic correctness:** The merged result (prefix + completion + suffix) must be valid, compilable code.
				- **Bracket matching:** Ensure all opened brackets/parentheses are properly closed (or left open if suffix closes them).
				- **High confidence:** When uncertain, prefer shorter, safer completions.
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

	public static String rerankSystemPrompt(int maxResultCount) {
		return """
				You are a software developer assistant
				Your task is to rank provided locations for relevance to the given instructions
				INFORMATIONS PROVIDED TO YOU:
				- Locations: A list of locations provided inside <locations></locations> tags
				- Instructions: What the locations should be ranked for provided inside <instructions></instructions> tags
				- Prefix: The code before the current edit location provided inside <prefix></prefix> tags
				- Suffix: The code after the current edit location provided inside <suffix></suffix> tags
				OUTPUT FORMAT:
				- Provide only a list of locations in order of relevance to the instructions
				- The top location should be the most relevant one
				- Start each line with "-" (a dash) followed by the location name
				- Do not provide any explanations or comments
				- Provide maximum %s locations
				- Provide the whole path (relative to the provided project root) of each location (not only the file name)
				EXAMPLE OUTPUT:
				<EXAMPLE>
				- path/to/location1
				- path/to/location2
				- path/to/location3
				</EXAMPLE>
				""".trim().formatted(maxResultCount);
	}

	public static String rerankPrompt(String locations, String instructions, String prefix, String suffix, String currentFileName) {
		return """
				Rerank the following locations for relevance to the given instructions.
				<locations>
				%s
				</locations>
				Here are the instructions:
				<instructions>
				%s
				</instructions>
				The current file is: %s
				Here is the code before the current edit location:
				<prefix>
				%s
				</prefix>
				Here is the code after the current edit location:
				<suffix>
				%s
				</suffix>
				""".trim().formatted(locations, instructions, prefix, suffix, currentFileName);
	}
}

package de.hetzge.eclipse.aicoder.inline;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.junit.jupiter.api.Test;

class InlineCompletionTest {

	@Test
	void test_1() throws BadLocationException {
		final String input = "package test123;\n"
				+ "\n"
				+ "public class Main {\n"
				+ "	public static void main(String[] args) {\n"
				+ "		for(<|cursor|>)\n"
				+ "	}\n"
				+ "}";
		final String output = "int i = 0; i < 10; i++) {\n"
				+ "			System.out.println(\"Hello World!\");\n"
				+ "		}\n"
				+ "		System.out.println(\"Hello World!\"";
		final String expected = input.replace("<|cursor|>", output);
		test0(input, output, expected, new Region(input.indexOf("<|cursor|>"), 1));
	}

	@Test
	void test_2() throws BadLocationException {
		final String input = "package test123;\n"
				+ "\n"
				+ "public class Main {\n"
				+ "	public static void main(String[] args) {\n"
				+ "		if(some.<|cursor|>())\n"
				+ "	}\n"
				+ "}";
		final String output = "someMethod";
		final String expected = input.replace("<|cursor|>", output);
		test0(input, output, expected, new Region(input.indexOf("<|cursor|>"), 0));
	}

	@Test
	void test_3() throws BadLocationException {
		final String input = "package test123;\n"
				+ "\n"
				+ "public class Main {\n"
				+ "	public static void main(String[] args) {\n"
				+ "		for (int i = 0; i < 10; i++) {<|cursor|>}\n"
				+ "	}\n"
				+ "}";
		final String output = "\n"
				+ "			System.out.println(\"Hello World!\");\n"
				+ "		";
		final String expected = input.replace("<|cursor|>", output);
		test0(input, output, expected, new Region(input.indexOf("<|cursor|>"), 1));
	}

	void test0(String input, String output, String expected, IRegion expectedRegion) throws BadLocationException {
		final Document document = new Document(input.replace("<|cursor|>", ""));
		final int offset = input.indexOf("<|cursor|>");
		final int lineIndex = (int) input.lines().filter(line -> !line.contains("<|cursor|>")).count();
		final InlineCompletion completion = InlineCompletion.create(null, document, offset, offset, lineIndex, output, 0, 0);
		completion.applyTo(document);
		assertEquals(expected, document.get());
		assertEquals(expectedRegion, completion.modelRegion());
	}
}

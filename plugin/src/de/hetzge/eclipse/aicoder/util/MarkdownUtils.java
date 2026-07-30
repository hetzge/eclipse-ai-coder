package de.hetzge.eclipse.aicoder.util;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

public final class MarkdownUtils {

	private MarkdownUtils() {
	}

	public static String markdownToHtml(String markdown) {
		final Parser parser = Parser.builder().build();
		final Node document = parser.parse(markdown);
		final HtmlRenderer renderer = HtmlRenderer.builder().build();
		return renderer.render(document);
	}
}

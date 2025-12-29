package de.hetzge.eclipse.aicoder.util;

import java.util.Map;

import com.hubspot.jinjava.Jinjava;
import com.hubspot.jinjava.JinjavaConfig;

public final class JinjaUtils {
	private static final Jinjava JINJAVA = new Jinjava(JinjavaConfig.newBuilder()
			.withTrimBlocks(true)
			.build());

	private JinjaUtils() {
	}

	public static String applyTemplate(String template, Map<String, ?> bindings) {
		return JINJAVA.render(template, bindings);
	}
}

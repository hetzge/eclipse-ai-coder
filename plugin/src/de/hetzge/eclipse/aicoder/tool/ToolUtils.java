package de.hetzge.eclipse.aicoder.tool;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IProject;

public final class ToolUtils {

	private ToolUtils() {
	}

	public static String createPathPrefixExamples(List<IProject> projects) {
		return projects.stream().map(it -> it.getName() + "/...").collect(Collectors.joining(", "));
	}

}

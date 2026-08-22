package de.hetzge.eclipse.aicoder.tool;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;

public final class ToolUtils {

	private ToolUtils() {
	}

	public static String createPathPrefixExamples(List<IProject> projects) {
		return projects.stream().map(it -> it.getName() + "/...").collect(Collectors.joining(", "));
	}

	/**
	 * Resolves the given path argument against the given project. If the path does
	 * not start with the project name but only one project is configured, the
	 * missing project prefix is tolerated and the project name is used as prefix.
	 * Returns an empty {@link Optional} if the path can not be used with the given
	 * project.
	 */
	public static Optional<IPath> resolvePath(String pathArgument, IProject project, List<IProject> projects) {
		final IPath projectPrefix = IPath.fromOSString(project.getName());
		if (pathArgument == null || pathArgument.isBlank() || pathArgument.trim().equals(".")) {
			return Optional.of(projectPrefix);
		}
		final IPath requestedPath = IPath.fromOSString(pathArgument);
		if (projectPrefix.isPrefixOf(requestedPath)) {
			return Optional.of(requestedPath);
		}
		if (projects.size() == 1) {
			// Be tolerant: ignore the missing project prefix because only one project is configured
			return Optional.of(projectPrefix.append(requestedPath));
		}
		return Optional.empty();
	}

}

package de.hetzge.eclipse.aicoder.util;

import java.io.IOException;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import de.hetzge.eclipse.aicoder.util.GitUtils.GitState;

public final class FileTreeUtils {

	private FileTreeUtils() {
	}

	public static String createResourceTreeString(IResource resource, List<String> whitelist, List<String> blacklist) {
		try (GitState gitState = GitUtils.getGitState(resource.getProject())) {
			final StringBuilder stringBuilder = new StringBuilder();
			appendResourceTreeString(stringBuilder, resource, gitState, 0, whitelist, blacklist);
			return stringBuilder.toString();
		} catch (final CoreException | IOException exception) {
			throw new RuntimeException("Error reading file tree", exception);
		}
	}

	private static boolean appendResourceTreeString(StringBuilder stringBuilder, IResource resource, GitState gitState, int depth, List<String> whitelist, List<String> blacklist) throws CoreException {
		if (gitState.isIgnored(resource)) {
			return false;
		}
		if (resource instanceof final IFile file) {
			if ((!whitelist.isEmpty() && whitelist.stream().noneMatch(pattern -> Utils.matches(pattern, file.getProjectRelativePath().toPath())))
					|| (!blacklist.isEmpty() && blacklist.stream().anyMatch(pattern -> Utils.matches(pattern, file.getProjectRelativePath().toPath())))) {
				return false;
			}
			final String indent = "  ".repeat(depth);
			stringBuilder.append(indent).append(file.getName()).append("\n");
			return true;
		}
		if (resource instanceof final IContainer container) {
			final String indent = "  ".repeat(depth);
			final StringBuilder childrenBuilder = new StringBuilder();
			boolean hasChildren = false;
			final IResource[] members = container.members();
			for (final IResource child : members) {
				final boolean childIncluded = appendResourceTreeString(childrenBuilder, child, gitState, depth + 1, whitelist, blacklist);
				hasChildren |= childIncluded;
			}
			if (hasChildren) {
				stringBuilder.append(indent).append(container.getName()).append("\n");
				stringBuilder.append(childrenBuilder);
				return true;
			}
		}
		return false;
	}

}
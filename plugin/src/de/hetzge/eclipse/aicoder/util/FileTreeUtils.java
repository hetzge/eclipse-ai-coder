package de.hetzge.eclipse.aicoder.util;

import java.io.IOException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import de.hetzge.eclipse.aicoder.util.GitUtils.GitState;

public final class FileTreeUtils {

	private FileTreeUtils() {
	}

	public static String createResourceTreeString(IResource resource) {
		try (GitState gitState = GitUtils.getGitState(resource.getProject())) {
			final StringBuilder stringBuilder = new StringBuilder();
			appendResourceTreeString(stringBuilder, resource, gitState, 0);
			return stringBuilder.toString();
		} catch (final CoreException | IOException exception) {
			throw new RuntimeException("Error reading file tree", exception);
		}
	}

	private static void appendResourceTreeString(StringBuilder stringBuilder, IResource resource, GitState gitState, int depth) throws CoreException {
		if (gitState.isIgnored(resource)) {
			return;
		}
		if (resource instanceof final IContainer container) {
			final String indent = "  ".repeat(depth);
			stringBuilder.append(indent).append(container.getName()).append("\n");
			final IResource[] members = container.members();
			for (final IResource child : members) {
				appendResourceTreeString(stringBuilder, child, gitState, depth + 1);
			}
		} else if (resource instanceof final IFile file) {
			final String indent = "  ".repeat(depth);
			stringBuilder.append(indent).append(file.getName()).append("\n");
		}
	}

}

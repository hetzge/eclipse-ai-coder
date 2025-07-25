package de.hetzge.eclipse.aicoder.context;

import java.time.Duration;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;

public class FileTreeContextEntry extends ContextEntry {

	public static final String PREFIX = "filetree";

	private final IProject project;

	public FileTreeContextEntry(IProject project, Duration creationDuration) {
		super(List.of(), creationDuration);
		this.project = project;
	}

	@Override
	public String getLabel() {
		return "File Tree";
	}

	@Override
	public String getContent(ContextContext context) {
		final StringBuilder stringBuilder = new StringBuilder();
		try {
			appendResourceTree(stringBuilder, this.project, 0);
		} catch (final CoreException exception) {
			throw new RuntimeException("Error reading file tree", exception);
		}
		return stringBuilder.toString();
	}

	@Override
	public ContextEntryKey getKey() {
		return new ContextEntryKey(PREFIX, this.project.getName());
	}

	private void appendResourceTree(StringBuilder sb, IResource resource, int depth) throws CoreException {
		final String indent = "  ".repeat(depth);
		sb.append(indent).append(resource.getName()).append("\n");

		if (resource instanceof final IContainer container) {
			for (final IResource child : container.members()) {
				// Skip .git, target, bin folders and hidden files
				if (child.getName().startsWith(".") ||
						child.getName().equals("target") ||
						child.getName().equals("bin")) {
					continue;
				}
				appendResourceTree(sb, child, depth + 1);
			}
		}
	}

	public static FileTreeContextEntry create(IEditorInput editorInput) throws CoreException {
		if (editorInput instanceof final IFileEditorInput fileEditorInput) {
			final IFile file = fileEditorInput.getFile();
			final IProject project = file.getProject();
			return new FileTreeContextEntry(project, Duration.ZERO);
		}
		throw new CoreException(null);
	}
}
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

import de.hetzge.eclipse.aicoder.util.ContextUtils;

public class FileTreeContextEntry extends ContextEntry {

	public static final String PREFIX = "FILE_TREE";

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
		try {
			final StringBuilder stringBuilder = new StringBuilder();
			appendResourceTree(stringBuilder, this.project, 0);
			return ContextUtils.codeTemplate("Project file tree", stringBuilder.toString());
		} catch (final CoreException exception) {
			throw new RuntimeException("Error reading file tree", exception);
		}
	}

	@Override
	public ContextEntryKey getKey() {
		return new ContextEntryKey(PREFIX, this.project.getName());
	}

	private void appendResourceTree(StringBuilder stringBuilder, IResource resource, int depth) throws CoreException {
		if (resource instanceof final IContainer container) {
			final IResource[] members = container.members();
			for (final IResource child : members) {
				final String indent = "  ".repeat(depth);
				stringBuilder.append(indent).append(child.getName());
				if (child instanceof IContainer) {
					stringBuilder.append("/");
				}
				stringBuilder.append("\n");
				if (child instanceof final IContainer childContainer) {
					appendResourceTree(stringBuilder, childContainer, depth + 1);
				}
			}
		}
	}

	public static ContextEntryFactory factory(IEditorInput editorInput) {
		return new ContextEntryFactory(PREFIX, () -> create(editorInput));
	}

	public static ContextEntry create(IEditorInput editorInput) throws CoreException {
		if (editorInput instanceof final IFileEditorInput fileEditorInput) {
			final IFile file = fileEditorInput.getFile();
			final IProject project = file.getProject();
			return new FileTreeContextEntry(project, Duration.ZERO);
		}
		throw new CoreException(null);
	}
}
package de.hetzge.eclipse.aicoder.context;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.util.ContextUtils;
import de.hetzge.eclipse.aicoder.util.GitUtils;
import de.hetzge.eclipse.aicoder.util.GitUtils.GitState;

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
			final GitState gitState = GitUtils.getGitState(this.project);
			final StringBuilder stringBuilder = new StringBuilder();
			appendResourceTree(stringBuilder, this.project, gitState, 0);
			return ContextUtils.codeTemplate("Project file tree", stringBuilder.toString());
		} catch (final CoreException | IOException exception) {
			throw new RuntimeException("Error reading file tree", exception);
		}
	}

	@Override
	public ContextEntryKey getKey() {
		return new ContextEntryKey(PREFIX, this.project.getName());
	}

	private void appendResourceTree(StringBuilder stringBuilder, IResource resource, GitState gitState, int depth) throws CoreException {
		if (gitState.isIgnored(resource)) {
			return;
		}
		if (resource instanceof final IContainer container) {
			final String indent = "  ".repeat(depth);
			stringBuilder.append(indent).append(container.getName()).append("\n");
			final IResource[] members = container.members();
			for (final IResource child : members) {
				appendResourceTree(stringBuilder, child, gitState, depth + 1);
			}
		} else if (resource instanceof final IFile file) {
			final String indent = "  ".repeat(depth);
			stringBuilder.append(indent).append(file.getName()).append("\n");
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
		if (editorInput == null) {
			throw new CoreException(new Status(IStatus.ERROR, AiCoderActivator.PLUGIN_ID, "Editor input is null"));
		}
		throw new CoreException(new Status(IStatus.ERROR, AiCoderActivator.PLUGIN_ID, "Unsupported editor input type: " + editorInput.getClass().getName()));
	}
}
package de.hetzge.eclipse.aicoder.context;

import java.time.Duration;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.util.ContextUtils;
import de.hetzge.eclipse.aicoder.util.FileTreeUtils;

public class FileTreeContextEntry extends ContextEntry {

	public static final String LABEL = "File Tree";
	public static final String PREFIX = "FILE_TREE";

	private final IProject project;

	public FileTreeContextEntry(IProject project, Duration creationDuration) {
		super(List.of(), creationDuration);
		this.project = project;
	}

	@Override
	public String getLabel() {
		return LABEL;
	}

	@Override
	public String getContent(ContextContext context) {
		return ContextUtils.codeTemplate("Project file tree", FileTreeUtils.createResourceTreeString(this.project));
	}

	@Override
	public ContextEntryKey getKey() {
		return new ContextEntryKey(PREFIX, this.project.getName());
	}

	public static ContextEntryFactory factory(IEditorInput editorInput) {
		return new ContextEntryFactory(PREFIX, () -> create(editorInput), () -> new EmptyContextEntry(PREFIX, LABEL, null));
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
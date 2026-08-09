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
import de.hetzge.eclipse.aicoder.config.ContextConfig.FileTreeConfig;
import de.hetzge.eclipse.aicoder.config.TaskConfig;
import de.hetzge.eclipse.aicoder.preferences.AiCoderPreferences;
import de.hetzge.eclipse.aicoder.util.ContextUtils;
import de.hetzge.eclipse.aicoder.util.FileTreeUtils;

public class FileTreeContextEntry extends ContextEntry {

	public static final String LABEL = "File Tree";
	public static final String PREFIX = "FILE_TREE";

	private final IProject project;
	private final List<String> whitelist;
	private final List<String> blacklist;

	public FileTreeContextEntry(IProject project, List<String> whitelist, List<String> blacklist, Duration creationDuration) {
		super(List.of(), creationDuration);
		this.project = project;
		this.whitelist = whitelist;
		this.blacklist = blacklist;
	}

	@Override
	public String getLabel() {
		return LABEL;
	}

	@Override
	public String getContent(ContextContext context) {
		return ContextUtils.codeTemplate("Project file tree", FileTreeUtils.createResourceTreeString(this.project, this.whitelist, this.blacklist));
	}

	@Override
	public ContextEntryKey getKey() {
		return new ContextEntryKey(PREFIX, this.project.getName());
	}

	public static ContextEntryFactory factory(IEditorInput editorInput, TaskConfig config) {
		return new ContextEntryFactory(PREFIX, () -> create(editorInput, config), () -> new EmptyContextEntry(PREFIX, LABEL, null));
	}

	public static ContextEntry create(IEditorInput editorInput, TaskConfig config) throws CoreException {
		final long before = System.currentTimeMillis();
		if (editorInput instanceof final IFileEditorInput fileEditorInput) {
			final IFile file = fileEditorInput.getFile();
			final IProject project = file.getProject();
			final List<String> whitelist = config.getContextConfig(PREFIX).map(FileTreeConfig.class::cast).map(FileTreeConfig::getWhitelist).orElseGet(() -> AiCoderPreferences.getFileTreeWhitelist());
			final List<String> blacklist = config.getContextConfig(PREFIX).map(FileTreeConfig.class::cast).map(FileTreeConfig::getBlacklist).orElseGet(() -> AiCoderPreferences.getFileTreeBlacklist());
			return new FileTreeContextEntry(project, whitelist, blacklist, Duration.ofMillis(System.currentTimeMillis() - before));
		}
		if (editorInput == null) {
			throw new CoreException(new Status(IStatus.ERROR, AiCoderActivator.PLUGIN_ID, "Editor input is null"));
		}
		throw new CoreException(new Status(IStatus.ERROR, AiCoderActivator.PLUGIN_ID, "Unsupported editor input type: " + editorInput.getClass().getName()));
	}
}

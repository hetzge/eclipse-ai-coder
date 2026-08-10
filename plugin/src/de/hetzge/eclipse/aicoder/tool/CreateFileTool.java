package de.hetzge.eclipse.aicoder.tool;

import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import mjson.Json;

public final class CreateFileTool extends Tool {

	private static Json prepareDefinition(List<IProject> projects) {
		return Json.object()
				.set("name", "create_file")
				.set("type", "function") // for responses api
				.set("description", "Creates a new file with the given content at a workspace-relative path, paths like " + ToolUtils.createPathPrefixExamples(projects))
				.set("parameters", Json.object()
						.set("type", "object")
						.set("properties", Json.object()
								.set("path", Json.object()
										.set("type", "string")
										.set("description", "Workspace-relative path of the new file, as example " + ToolUtils.createPathPrefixExamples(projects) + "."))
								.set("content", Json.object()
										.set("type", "string")
										.set("description", "The full content to write to the new file. Defaults to an empty file.")
										.set("default", ""))
								.set("overwrite", Json.object()
										.set("type", "boolean")
										.set("description", "Whether to overwrite the file if it already exists. Defaults to false.")
										.set("default", false)))
						.set("required", Json.array().add("path")));
	}

	private final List<IProject> projects;
	private final FileSystem fileSystem;

	public CreateFileTool(List<IProject> projects, FileSystem fileSystem) {
		super(prepareDefinition(projects));
		this.projects = projects;
		this.fileSystem = fileSystem;
		if (this.projects.isEmpty()) {
			throw new IllegalArgumentException("At least one project must be provided.");
		}
	}

	@Override
	public String execute(IProgressMonitor monitor, Json arguments) {
		final String pathArg = arguments.at("path", "").asString();
		final String content = arguments.at("content", "").asString();
		final boolean overwrite = arguments.at("overwrite", false).asBoolean();
		if (pathArg == null || pathArg.isBlank()) {
			return "Error: path argument is required.";
		}
		if (content == null) {
			return "Error: content argument must be a string.";
		}
		if (this.projects.stream().noneMatch(it -> pathArg.startsWith(it.getName()))) {
			return "Error: path ('" + pathArg + "') must be relative to the workspace (as example " + ToolUtils.createPathPrefixExamples(this.projects) + ").";
		}
		try {
			final IPath path = IPath.fromOSString(pathArg);
			final IWorkspaceRoot workspaceRoot = this.projects.get(0).getWorkspace().getRoot();
			final IResource resource = workspaceRoot.findMember(path);
			if (resource != null && resource.exists() && resource instanceof IContainer) {
				return "Error: path is a folder, not a file: " + pathArg;
			}
			final boolean existsInWorkspace = resource != null && resource.exists();
			final boolean existsInSession = this.fileSystem.contains(path) && !this.fileSystem.readFile(path).isEmpty();
			if ((existsInWorkspace || existsInSession) && !overwrite) {
				return "Error: file already exists (set overwrite=true to replace it): " + pathArg;
			}
			this.fileSystem.putFile(path, content);
			return "Successfully created file " + pathArg;
		} catch (final Exception exception) {
			AiCoderActivator.log().error("Error creating file: " + pathArg, exception);
			return "Error creating file: " + exception.getMessage();
		}
	}
}

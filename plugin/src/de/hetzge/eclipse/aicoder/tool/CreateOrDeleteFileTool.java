package de.hetzge.eclipse.aicoder.tool;

import java.util.List;
import java.util.Optional;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import mjson.Json;

public final class CreateOrDeleteFileTool extends Tool {

	private static Json prepareDefinition(List<IProject> projects) {
		return Json.object()
				.set("name", "create_or_delete_file")
				.set("type", "function") // for responses api
				.set("description", "Creates a new file with the given content at a workspace-relative path, or deletes the file if no content is provided. Paths like " + ToolUtils.createPathPrefixExamples(projects))
				.set("parameters", Json.object()
						.set("type", "object")
						.set("properties", Json.object()
								.set("path", Json.object()
										.set("type", "string")
										.set("description", "Workspace-relative path of the new file, as example " + ToolUtils.createPathPrefixExamples(projects) + "."))
								.set("content", Json.object()
										.set("type", "string")
										.set("description", "The full content to write to the new file. If no content is provided, the file is deleted."))
								.set("overwrite", Json.object()
										.set("type", "boolean")
										.set("description", "Whether to overwrite the file if it already exists. Defaults to false.")
										.set("default", false)))
						.set("required", Json.array().add("path")));
	}

	private final List<IProject> projects;
	private final FileSystem fileSystem;

	public CreateOrDeleteFileTool(List<IProject> projects, FileSystem fileSystem) {
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
		final boolean overwrite = arguments.at("overwrite", false).asBoolean();
		final boolean delete = !arguments.has("content") || arguments.at("content").isNull();
		if (pathArg == null || pathArg.isBlank()) {
			return "Error: path argument is required.";
		}
		// Be tolerant: if only one project is configured, a missing project prefix in the path is ignored
		final Optional<IPath> pathOptional = this.projects.stream()
				.map(project -> ToolUtils.resolvePath(pathArg, project, this.projects))
				.flatMap(Optional::stream)
				.findFirst();
		if (pathOptional.isEmpty()) {
			return "Error: path ('" + pathArg + "') must be relative to the workspace (as example " + ToolUtils.createPathPrefixExamples(this.projects) + ").";
		}
		final IPath path = pathOptional.get();
		try {
			final IWorkspaceRoot workspaceRoot = this.projects.get(0).getWorkspace().getRoot();
			final IResource resource = workspaceRoot.findMember(path);
			if (resource != null && resource.exists() && resource instanceof IContainer) {
				return "Error: path is a folder, not a file: " + pathArg;
			}
			final boolean existsInWorkspace = resource != null && resource.exists();
			final boolean existsInSession = this.fileSystem.contains(path) && !this.fileSystem.readFile(path).isEmpty();
			if (delete) {
				if (!existsInWorkspace && !existsInSession) {
					return "Error: file does not exist: " + pathArg;
				}
				this.fileSystem.putFile(path, "");
				return "Successfully deleted file " + pathArg;
			}
			final String content = arguments.at("content", "").asString();
			if (content == null) {
				return "Error: content argument must be a string.";
			}
			if ((existsInWorkspace || existsInSession) && !overwrite) {
				return "Error: file already exists (set overwrite=true to replace it): " + pathArg;
			}
			this.fileSystem.putFile(path, content);
			return "Successfully created file " + pathArg;
		} catch (final Exception exception) {
			AiCoderActivator.log().error("Error creating or deleting file: " + pathArg, exception);
			return "Error creating or deleting file: " + exception.getMessage();
		}
	}
}

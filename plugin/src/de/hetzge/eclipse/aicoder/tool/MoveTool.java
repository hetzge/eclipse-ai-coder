package de.hetzge.eclipse.aicoder.tool;

import java.util.List;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import mjson.Json;

// TODO WIP
public final class MoveTool extends Tool {

	public static final Json DEFINITION = Json.object()
			.set("name", "move")
			.set("description", "Renames, moves, copies, or deletes files and directories.")
			.set("parameters", Json.object()
					.set("type", "object")
					.set("properties", Json.object()
							.set("project", Json.object()
									.set("type", "string")
									.set("description", "Project name to move the file in"))
							.set("source", Json.object()
									.set("type", "string")
									.set("description", "The source file or directory to move."))
							.set("destination", Json.object()
									.set("type", "string")
									.set("description", "The destination file or directory to move to."))
							.set("operation", Json.object()
									.set("type", "string")
									.set("description", "The operation to perform: move, copy, rename, or delete.")
									.set("enum", Json.array().add("move").add("copy").add("rename").add("delete"))))
					.set("required", Json.array().add("source").add("operation")));

	private static Json prepareDefinition(List<IProject> projects) {
		final Json definition = Json.read(DEFINITION.toString());
		if (projects.size() == 1) {
			definition.at("parameters").at("properties").delAt("project");
		} else {
			definition.at("parameters").at("properties").at("project").set("enum", projects.stream().map(IProject::getName).toList());
		}
		return definition;
	}

	private final List<IProject> projects;

	public MoveTool(List<IProject> projects) {
		super(prepareDefinition(projects));
		this.projects = projects;
	}

	@Override
	public String execute(IProgressMonitor monitor, Json arguments) {
		final String sourceArg = arguments.at("source").asString();
		final String operation = arguments.at("operation").asString();
		final String destinationArg = arguments.has("destination") ? arguments.at("destination").asString() : null;

		if (sourceArg == null || sourceArg.isBlank()) {
			return "Error: source argument is required.";
		}
		if (operation == null || operation.isBlank()) {
			return "Error: operation argument is required.";
		}

		final IProject project = this.projects.get(0); // Use the first project

		try {
			switch (operation) {
			case "delete": {
				final IResource resource = project.findMember(IPath.fromOSString(sourceArg));
				if (resource == null || !resource.exists()) {
					return "Error: Source not found: " + sourceArg;
				}
				resource.delete(IResource.FORCE, new NullProgressMonitor());
				return "Successfully deleted " + sourceArg;
			}
			case "move":
			case "copy":
			case "rename": {
				if (destinationArg == null || destinationArg.isBlank()) {
					return "Error: destination argument is required for operation: " + operation;
				}
				final IResource sourceResource = project.findMember(IPath.fromOSString(sourceArg));
				if (sourceResource == null || !sourceResource.exists()) {
					return "Error: Source not found: " + sourceArg;
				}
				final IPath destinationPath = IPath.fromOSString(destinationArg);
				final IResource destinationResource = project.findMember(destinationPath);
				if (destinationResource != null && destinationResource.exists()) {
					return "Error: Destination already exists: " + destinationArg;
				}
				// Ensure parent directories exist
				final IPath parentPath = destinationPath.removeLastSegments(1);
				if (!parentPath.isEmpty()) {
					final IFolder parentFolder = project.getFolder(parentPath);
					if (!parentFolder.exists()) {
						parentFolder.create(IResource.FORCE | IResource.DERIVED, true, new NullProgressMonitor());
					}
				}
				if ("rename".equals(operation)) {
					sourceResource.move(destinationPath, IResource.FORCE, new NullProgressMonitor());
				} else if ("copy".equals(operation)) {
					sourceResource.copy(destinationPath, IResource.FORCE, new NullProgressMonitor());
				} else if ("move".equals(operation)) {
					sourceResource.move(destinationPath, IResource.FORCE, new NullProgressMonitor());
				}
				return "Successfully " + operation + "d " + sourceArg + " to " + destinationArg;
			}
			default:
				return "Error: Unsupported operation: " + operation;
			}
		} catch (final CoreException exception) {
			return "Error executing " + operation + " operation: " + exception.getMessage();
		}
	}
}
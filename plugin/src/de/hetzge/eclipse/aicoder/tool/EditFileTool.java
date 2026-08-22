package de.hetzge.eclipse.aicoder.tool;

import java.util.List;
import java.util.Optional;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import mjson.Json;

public final class EditFileTool extends Tool {

	private static Json prepareDefinition(List<IProject> projects) {
		return Json.object()
				.set("name", "edit_file")
				.set("type", "function") // for responses api
				.set("description", "Edits a file in the project by replacing old text with new text. The old text must appear exactly once in the file.")
				.set("parameters", Json.object()
						.set("type", "object")
						.set("properties", Json.object()
								.set("path", Json.object()
										.set("type", "string")
										.set("description", "Local file path (relative to workspace root, as example " + ToolUtils.createPathPrefixExamples(projects) + ")."))
								.set("old_text", Json.object()
										.set("type", "string")
										.set("description", "The exact text to replace. Must be unique in the file."))
								.set("new_text", Json.object()
										.set("type", "string")
										.set("description", "The new text to insert in place of old_text.")))
						.set("required", Json.array().add("path").add("old_text").add("new_text")));
	}

	private final List<IProject> projects;
	private final FileSystem fileSystem;

	public EditFileTool(List<IProject> projects, FileSystem fileSystem) {
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
		final String oldText = arguments.at("old_text", "").asString();
		final String newText = arguments.at("new_text", "").asString();
		if (pathArg == null || pathArg.isBlank()) {
			return "Error: path argument is required.";
		}
		if (oldText == null || oldText.isEmpty()) {
			return "Error: old_text argument is required.";
		}
		// Be tolerant: if only one project is configured, a missing project prefix in the path is ignored
		final Optional<IPath> pathOptional = ToolUtils.resolvePath(pathArg, this.projects.get(0), this.projects);
		if (pathOptional.isEmpty()) {
			return "Error: path ('" + pathArg + "') must be relative to the workspace (as example " + ToolUtils.createPathPrefixExamples(this.projects) + ").";
		}
		final IPath path = pathOptional.get();
		try {
			final String content = this.fileSystem.readFile(path);
			final int index = content.indexOf(oldText);
			if (index == -1) {
				return "Error: old_text not found in file.";
			}
			final int nextIndex = content.indexOf(oldText, index + oldText.length());
			if (nextIndex != -1) {
				return "Error: old_text appears multiple times in the file, must be unique.";
			}
			final String newContent = content.substring(0, index) + newText + content.substring(index + oldText.length());
			this.fileSystem.putFile(path, newContent);
			return "Successfully replaced text in " + pathArg;
		} catch (final Exception exception) {
			AiCoderActivator.log().error("Error editing file: " + pathArg, exception);
			return "Error editing file: " + exception.getMessage();
		}
	}
}

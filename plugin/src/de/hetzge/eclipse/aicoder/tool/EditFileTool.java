package de.hetzge.eclipse.aicoder.tool;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import mjson.Json;

public final class EditFileTool extends Tool {

	public static final Json DEFINITION = Json.object()
			.set("name", "edit_file")
			.set("description", "Edits a file in the project by replacing old text with new text. The old text must appear exactly once in the file.")
			.set("parameters", Json.object()
					.set("type", "object")
					.set("properties", Json.object()
							.set("path", Json.object()
									.set("type", "string")
									.set("description", "Local file path (relative to project root)."))
							.set("old_text", Json.object()
									.set("type", "string")
									.set("description", "The exact text to replace. Must be unique in the file."))
							.set("new_text", Json.object()
									.set("type", "string")
									.set("description", "The new text to insert in place of old_text.")))
					.set("required", Json.array().add("path").add("old_text").add("new_text")));

	private final IProject project;

	public EditFileTool(IProject project) {
		super(DEFINITION);
		this.project = project;
	}

	@Override
	public String execute(Json arguments) {
		final String pathArg = arguments.at("path").asString();
		final String oldText = arguments.at("old_text").asString();
		final String newText = arguments.at("new_text").asString();

		if (pathArg == null || pathArg.isBlank()) {
			return "Error: path argument is required.";
		}
		if (oldText == null || oldText.isEmpty()) {
			return "Error: old_text argument is required.";
		}
		if (newText == null) {
			// Allow empty new text
		}

		final IFile file;
		try {
			final IResource resource = this.project.findMember(IPath.fromOSString(pathArg));
			if (resource == null || !resource.exists() || !(resource instanceof IFile)) {
				return "Error: File not found or not a file: " + pathArg;
			}
			file = (IFile) resource;
		} catch (final Exception exception) {
			AiCoderActivator.log().error("Error resolving file: " + pathArg, exception);
			return "Error resolving file: " + exception.getMessage();
		}

		try {
			final String content = readFile(file);
			final int index = content.indexOf(oldText);
			if (index == -1) {
				return "Error: old_text not found in file.";
			}
			final int nextIndex = content.indexOf(oldText, index + oldText.length());
			if (nextIndex != -1) {
				return "Error: old_text appears multiple times in the file, must be unique.";
			}

			final String newContent = content.substring(0, index) + newText + content.substring(index + oldText.length());
			writeFile(file, newContent);
			return "Successfully replaced text in " + pathArg;
		} catch (final Exception exception) {
			AiCoderActivator.log().error("Error editing file: " + pathArg, exception);
			return "Error editing file: " + exception.getMessage();
		}
	}

	private String readFile(IFile file) throws CoreException, IOException {
		try (final InputStream inputStream = file.getContents()) {
			return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	private void writeFile(IFile file, String content) throws CoreException, IOException {
		try (final InputStream inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))) {
			file.setContents(inputStream, IFile.FORCE, new NullProgressMonitor());
		}
	}

}
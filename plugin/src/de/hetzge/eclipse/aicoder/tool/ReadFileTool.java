package de.hetzge.eclipse.aicoder.tool;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import mjson.Json;

public final class ReadFileTool extends Tool {

	public static final Json DEFINITION = Json.object()
			.set("name", "read_file")
			.set("description", "Reads a file from the project")
			.set("parameters", Json.object()
					.set("type", "object")
					.set("properties", Json.object()
							.set("project", Json.object()
									.set("type", "string")
									.set("description", "Project name to read the file from"))
							.set("path", Json.object()
									.set("type", "string")
									.set("description", "Local file path (relative to project root)."))
							.set("start_line", Json.object()
									.set("type", "integer")
									.set("description", "1-indexed line number to begin reading. Default: 1")
									.set("default", 1))
							.set("end_line", Json.object()
									.set("type", "integer")
									.set("description", "Inclusive 1-indexed line number to end reading. If omitted, reads until max_lines is reached or EOF."))
							.set("max_lines", Json.object()
									.set("type", "integer")
									.set("description", "Maximum lines to return if end_line is not set. Default: 2000")
									.set("default", 2000))
							.set("include_line_numbers", Json.object()
									.set("type", "boolean")
									.set("description", "Whether to prepend each line with its line number (e.g., '42: '). Ignored if byte_offset is set. Default: false")
									.set("default", false)))
					.set("required", Json.array().add("path")));

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

	public ReadFileTool(List<IProject> projects) {
		super(prepareDefinition(projects));
		this.projects = projects;
		if (this.projects.isEmpty()) {
			throw new IllegalArgumentException("At least one project must be provided.");
		}
	}

	@Override
	public String execute(Json arguments) {
		final String pathArg = arguments.at("path").asString();
		final int startLine = arguments.has("start_line") ? arguments.at("start_line").asInteger() : 1;
		final Integer endLine = arguments.has("end_line") && !arguments.at("end_line").isNull() ? arguments.at("end_line").asInteger() : null;
		final int maxLines = arguments.has("max_lines") ? arguments.at("max_lines").asInteger() : 2000;
		final boolean includeLineNumbers = arguments.has("include_line_numbers") && arguments.at("include_line_numbers").asBoolean();
		final Optional<IProject> projectOptional = arguments.has("project")
				? this.projects.stream().filter(it -> it.getName().equals(arguments.at("project").asString())).findFirst()
				: this.projects.size() == 1 ? Optional.of(this.projects.get(0)) : Optional.empty();
		if (projectOptional.isEmpty()) {
			return "Error: Project not found: " + arguments.at("project").asString() + ". Available projects: " + this.projects.stream().map(IProject::getName).toList();
		}
		final IProject project = projectOptional.get();

		final String content;
		try {
			content = readFile(project, pathArg, StandardCharsets.UTF_8);
		} catch (final FileNotFoundException exception) {
			return "Error: File not found: " + pathArg;
		} catch (final Exception exception) {
			AiCoderActivator.log().error("Error reading file: " + pathArg, exception);
			return "Error reading file: " + exception.getMessage();
		}
		final List<String> lines = content.lines().toList();
		final int fromLine = Math.max(1, startLine);
		final int toLine = endLine != null ? Math.min(endLine, lines.size()) : Math.min(lines.size(), fromLine - 1 + Math.max(0, maxLines));
		final StringBuilder builder = new StringBuilder();
		for (int lineNumber = fromLine; lineNumber <= toLine; lineNumber++) {
			if (lineNumber < 1 || lineNumber > lines.size()) {
				break;
			}
			if (builder.length() > 0) {
				builder.append('\n');
			}
			if (includeLineNumbers) {
				builder.append(lineNumber).append(": ");
			}
			builder.append(lines.get(lineNumber - 1));
		}
		return builder.toString();
	}

	private String readFile(IProject project, String source, Charset charset) throws IOException {
		final IPath path = IPath.fromOSString(source);
		final IResource resource = path.isAbsolute() ? project.getWorkspace().getRoot().findMember(path) : project.getFile(source);
		if (resource == null || !resource.exists()) {
			throw new FileNotFoundException(source);
		}
		try (final InputStream inputStream = ((IFile) resource).getContents()) {
			return new String(inputStream.readAllBytes(), charset);
		} catch (final CoreException exception) {
			throw new IOException("Error reading file: " + exception.getMessage(), exception);
		}
	}

}
package de.hetzge.eclipse.aicoder.tool;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

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
					.set("required", Json.array().add("source")));

	private final IProject project;

	public ReadFileTool(IProject project) {
		super(DEFINITION);
		this.project = project;
	}

	@Override
	public String execute(Json arguments) {
		final String source = arguments.at("source").asString();
		final int startLine = arguments.has("start_line") ? arguments.at("start_line").asInteger() : 1;
		final Integer endLine = arguments.has("end_line") && !arguments.at("end_line").isNull() ? arguments.at("end_line").asInteger() : null;
		final int maxLines = arguments.has("max_lines") ? arguments.at("max_lines").asInteger() : 2000;
		final boolean includeLineNumbers = arguments.has("include_line_numbers") && arguments.at("include_line_numbers").asBoolean();
		final String content;
		try {
			content = readFile(source, StandardCharsets.UTF_8);
		} catch (final Exception exception) {
			AiCoderActivator.log().error("Error reading file: " + source, exception);
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

	private String readFile(String source, Charset charset) throws IOException {
		final IPath path = IPath.fromOSString(source);
		final IResource resource = path.isAbsolute() ? this.project.getWorkspace().getRoot().findMember(path) : this.project.getFile(source);
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
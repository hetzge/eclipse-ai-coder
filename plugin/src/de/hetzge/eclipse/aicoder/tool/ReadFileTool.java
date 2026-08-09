package de.hetzge.eclipse.aicoder.tool;

import java.io.IOException;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.preferences.AiCoderPreferences;
import mjson.Json;

public final class ReadFileTool extends Tool {

	private static Json prepareDefinition(List<IProject> projects) {
		return Json.object()
				.set("name", "read_file")
				.set("description", "Reads a file from the project")
				.set("parameters", Json.object()
						.set("type", "object")
						.set("properties", Json.object()
								.set("path", Json.object()
										.set("type", "string")
										.set("description", "Local file path (relative to workspace root, as example " + ToolUtils.createPathPrefixExamples(projects) + ")")
										.set("start_line", Json.object()
												.set("type", "integer")
												.set("description", "1-indexed line number to begin reading. Default: 1")
												.set("default", 1))
										.set("end_line", Json.object()
												.set("type", "integer")
												.set("description", "Inclusive 1-indexed line number to end reading. If omitted, reads until max_lines is reached or EOF."))
										.set("max_lines", Json.object()
												.set("type", "integer")
												.set("description", "Maximum lines to return if end_line is not set. Default: " + AiCoderPreferences.getReadFileDefaultMaxLineCount())
												.set("default", AiCoderPreferences.getReadFileDefaultMaxLineCount()))
										.set("include_line_numbers", Json.object()
												.set("type", "boolean")
												.set("description", "Whether to prepend each line with its line number (e.g., '42: '). Ignored if byte_offset is set. Default: false")
												.set("default", false)))
								.set("required", Json.array().add("path"))));
	}

	private final List<IProject> projects;
	private final FileSystem fileSystem;

	public ReadFileTool(List<IProject> projects, FileSystem fileSystem) {
		super(prepareDefinition(projects));
		this.projects = projects;
		this.fileSystem = fileSystem;
		if (this.projects.isEmpty()) {
			throw new IllegalArgumentException("At least one project must be provided.");
		}
	}

	@Override
	public String execute(IProgressMonitor monitor, Json arguments) {
		final String pathArg = arguments.at("path").asString();
		final int startLine = arguments.has("start_line") ? arguments.at("start_line").asInteger() : 1;
		final Integer endLine = arguments.has("end_line") && !arguments.at("end_line").isNull() ? arguments.at("end_line").asInteger() : null;
		final int maxLines = arguments.has("max_lines") ? arguments.at("max_lines").asInteger() : AiCoderPreferences.getReadFileDefaultMaxLineCount();
		final boolean includeLineNumbers = arguments.has("include_line_numbers") && arguments.at("include_line_numbers").asBoolean();
		if (pathArg == null || pathArg.isBlank()) {
			return "Error: path argument is required.";
		}
		if (this.projects.stream().noneMatch(it -> pathArg.startsWith(it.getName()))) {
			return "Error: path ('" + pathArg + "') must be relative to the workspace (as example " + ToolUtils.createPathPrefixExamples(this.projects) + ").";
		}
		String content;
		try {
			content = this.fileSystem.readFile(IPath.fromOSString(pathArg));
		} catch (final IOException exception) {
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
		if (endLine == null && fromLine <= lines.size() && toLine < lines.size()) {
			if (builder.length() > 0) {
				builder.append('\n');
			}
			builder.append("[More lines were skipped because max_lines was reached.]");
		}
		return builder.toString();
	}
}
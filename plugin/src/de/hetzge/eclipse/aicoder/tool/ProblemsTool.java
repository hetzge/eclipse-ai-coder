package de.hetzge.eclipse.aicoder.tool;

import java.util.List;
import java.util.Optional;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import de.hetzge.eclipse.aicoder.util.EclipseUtils;
import mjson.Json;

public final class ProblemsTool extends Tool {

	private static Json prepareDefinition(List<IProject> projects) {
		return Json.object()
				.set("name", "problems")
				.set("type", "function") // for responses api
				.set("description", "Gets all problem markers for a file or folder.")
				.set("parameters", Json.object()
						.set("type", "object")
						.set("properties", Json.object()
								.set("path", Json.object()
										.set("type", "string")
										.set("description", "Workspace-relative path to a file or folder, as example " + ToolUtils.createPathPrefixExamples(projects) + ".")))
						.set("required", Json.array().add("path")));
	}

	private final List<IProject> projects;

	public ProblemsTool(List<IProject> projects) {
		super(prepareDefinition(projects));
		this.projects = projects;
		if (this.projects.isEmpty()) {
			throw new IllegalArgumentException("At least one project must be provided.");
		}
	}

	@Override
	public String execute(IProgressMonitor monitor, Json arguments) {
		final String path = arguments.at("path", "").asString();
		if (path.isBlank()) {
			return "Error: path argument is required.";
		}

		// Be tolerant: if only one project is configured, a missing project prefix in the path is ignored
		final Optional<IPath> pathOptional = ToolUtils.resolvePath(path, this.projects.get(0), this.projects);
		if (pathOptional.isEmpty()) {
			return "Error: path ('" + path + "') must be relative to the workspace (as example " + ToolUtils.createPathPrefixExamples(this.projects) + ").";
		}

		try {
			final List<IMarker> markers = EclipseUtils.getProblemMarkers(pathOptional.get());
			if (markers.isEmpty()) {
				return "No problems found.";
			}

			final StringBuilder result = new StringBuilder();
			for (final IMarker marker : markers) {
				formatMarker(result, marker);
			}
			return result.toString();
		} catch (final CoreException exception) {
			return "Error retrieving problems: " + exception.getMessage();
		}
	}

	public static String formatMarker(IMarker marker) {
		final StringBuilder result = new StringBuilder();
		formatMarker(result, marker);
		return result.toString();
	}

	private static void formatMarker(final StringBuilder result, final IMarker marker) {
		if (result.length() > 0) {
			result.append('\n');
		}
		final String severity = switch (marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO)) {
		case IMarker.SEVERITY_ERROR -> "ERROR";
		case IMarker.SEVERITY_WARNING -> "WARNING";
		default -> "INFO";
		};
		result.append(severity)
				.append(' ')
				.append(marker.getResource().getFullPath().toPortableString())
				.append(':')
				.append(marker.getAttribute(IMarker.LINE_NUMBER, -1))
				.append(": ")
				.append(marker.getAttribute(IMarker.MESSAGE, ""));
	}
}
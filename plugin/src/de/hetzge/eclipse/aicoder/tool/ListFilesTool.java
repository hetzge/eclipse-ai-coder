package de.hetzge.eclipse.aicoder.tool;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import mjson.Json;

public final class ListFilesTool extends Tool {

	public static final Json DEFINITION = Json.object()
			.set("name", "list_files")
			.set("description", "Lists files in the project")
			.set("parameters", Json.object()
					.set("type", "object")
					.set("properties", Json.object()
							.set("project", Json.object()
									.set("type", "string")
									.set("description", "Project name to list files from"))
							.set("path", Json.object()
									.set("type", "string")
									.set("description", "The path to list files from. Default: project root."))
							.set("search_pattern", Json.object()
									.set("type", "string")
									.set("description", "The pattern to search for in file names. Default: empty string."))
							.set("max_results", Json.object()
									.set("type", "integer")
									.set("description", "The maximum number of results to return. Default: 100."))));

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

	public ListFilesTool(List<IProject> projects) {
		super(prepareDefinition(projects));
		this.projects = projects;
		if (this.projects.isEmpty()) {
			throw new IllegalArgumentException("At least one project must be provided.");
		}
	}

	@Override
	public String execute(Json arguments) {
		final StringBuilder sb = new StringBuilder();
		if (arguments.has("project")) {
			final String projectName = arguments.at("project").asString();
			final Optional<IProject> projectOptional = this.projects.stream()
					.filter(it -> it.getName().equals(projectName))
					.findFirst();
			if (projectOptional.isEmpty()) {
				return "Error: Project not found: " + projectName + ". Available projects: " + this.projects.stream().map(IProject::getName).toList();
			}
			final IProject project = projectOptional.get();
			sb.append(execute(project, arguments));
		} else {
			for (final IProject project : this.projects) {
				if (sb.length() > 0) {
					sb.append("\n");
				}
				sb.append("Project: ").append(project.getName()).append("\n");
				for (final String line : execute(project, arguments).lines().toList()) {
					sb.append("  ").append(line).append("\n");
				}
			}
		}
		return sb.toString();
	}

	private String execute(IProject project, Json arguments) {
		final String resolvedPath = arguments.has("path") ? arguments.at("path").asString() : "";
		final String resolvedSearchPattern = arguments.has("search_pattern") ? arguments.at("search_pattern").asString() : "";
		final int resolvedMaxResults = arguments.has("max_results") ? arguments.at("max_results").asInteger() : 100;

		if (!resolvedSearchPattern.isBlank()) {
			final String patternLower = resolvedSearchPattern.toLowerCase();
			final List<IResource> matches = new ArrayList<>();
			final Deque<IContainer> stack = new ArrayDeque<>();
			stack.push(project);
			while (!stack.isEmpty() && matches.size() < resolvedMaxResults) {
				final IContainer current = stack.pop();
				try {
					for (final IResource member : current.members()) {
						if (member.getName().toLowerCase().contains(patternLower)) {
							matches.add(member);
							if (matches.size() >= resolvedMaxResults) {
								break;
							}
						}
						if (member instanceof IContainer) {
							stack.push((IContainer) member);
						}
					}
				} catch (final CoreException e) {
					// skip inaccessible containers
				}
			}
			matches.sort(Comparator.comparing(it -> it.getProjectRelativePath().toPortableString()));
			final int limit = Math.min(matches.size(), resolvedMaxResults);
			final StringBuilder sb = new StringBuilder();
			for (int i = 0; i < limit; i++) {
				if (i > 0) {
					sb.append("\n");
				}
				final IResource member = matches.get(i);
				sb.append(member instanceof IContainer ? "[DIR] " : "[FILE] ")
						.append(member.getProjectRelativePath().toPortableString());
			}
			return sb.toString();
		}

		IContainer container;
		if (resolvedPath.isEmpty()) {
			container = project;
		} else {
			final IResource resource = project.findMember(IPath.fromOSString(resolvedPath));
			if (resource == null || !resource.exists()) {
				return "Error: Path not found: " + resolvedPath;
			}
			if (!(resource instanceof IContainer)) {
				return "Error: Path is not a directory: " + resolvedPath;
			}
			container = (IContainer) resource;
		}

		try {
			final IResource[] members = container.members();
			final List<IResource> filtered = new ArrayList<>();
			for (final IResource member : members) {
				if (resolvedSearchPattern.isEmpty() || member.getName().toLowerCase().contains(resolvedSearchPattern.toLowerCase())) {
					filtered.add(member);
				}
			}
			filtered.sort(Comparator.comparing(IResource::getName));
			final int limit = Math.min(filtered.size(), resolvedMaxResults);
			final StringBuilder sb = new StringBuilder();
			for (int i = 0; i < limit; i++) {
				final IResource member = filtered.get(i);
				if (sb.length() > 0) {
					sb.append("\n");
				}
				sb.append(member instanceof IContainer ? "[DIR] " : "[FILE] ").append(member.getProjectRelativePath().toPortableString());
			}
			return sb.toString();
		} catch (final CoreException e) {
			return "Error listing files: " + e.getMessage();
		}
	}
}

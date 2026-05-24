package de.hetzge.eclipse.aicoder.tool;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.quicksearch.ProjectSearchUtils;
import de.hetzge.eclipse.aicoder.quicksearch.SearchOptions;
import de.hetzge.eclipse.aicoder.quicksearch.SearchResult;
import mjson.Json;

public final class SearchTool extends Tool {

	public static final Json DEFINITION = Json.object()
			.set("name", "search")
			.set("description", "Searches the project for pattern in the source code")
			.set("parameters", Json.object()
					.set("type", "object")
					.set("properties", Json.object()
							.set("pattern", Json.object()
									.set("type", "string")
									.set("description", "The pattern to search for."))
							.set("project", Json.object()
									.set("type", "string")
									.set("description", "Project name to search in")))
					.set("required", Json.array().add("pattern")));

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

	public SearchTool(List<IProject> projects) {
		super(prepareDefinition(projects));
		this.projects = projects;
		if (this.projects.isEmpty()) {
			throw new IllegalArgumentException("At least one project must be provided.");
		}
	}

	@Override
	public String execute(Json arguments) {
		final String pattern = arguments.at("pattern").asString();
		try {
			final StringBuilder builder = new StringBuilder();
			if (arguments.has("project")) {
				final String projectName = arguments.at("project").asString();
				final IProject project = this.projects.stream()
						.filter(it -> it.getName().equals(projectName))
						.findFirst()
						.orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectName));
				final List<SearchResult> results = ProjectSearchUtils.search(project, SearchOptions.builder(pattern).build(), new NullProgressMonitor());
				for (final SearchResult result : results) {
					if (builder.length() > 0) {
						builder.append('\n');
					}
					builder
							.append(result.getFile().getProjectRelativePath().toOSString())
							.append(':')
							.append(result.getLineNumber())
							.append(':')
							.append(result.getColumnStart())
							.append(':')
							.append(result.getColumnEnd())
							.append(':')
							.append(result.getLineContent());
				}
			} else {
				for (final IProject project : this.projects) {
					final List<SearchResult> results = ProjectSearchUtils.search(project, SearchOptions.builder(pattern).build(), new NullProgressMonitor());
					for (final SearchResult result : results) {
						if (builder.length() > 0) {
							builder.append('\n');
						}
						builder
								.append(project.getName())
								.append(':')
								.append(result.getFile().getProjectRelativePath().toOSString())
								.append(':')
								.append(result.getLineNumber())
								.append(':')
								.append(result.getColumnStart())
								.append(':')
								.append(result.getColumnEnd())
								.append(':')
								.append(result.getLineContent());
					}
				}
			}
			return builder.toString();
		} catch (final Exception exception) {
			AiCoderActivator.log().error("Error searching project", exception);
			return "Error searching project: " + exception.getMessage();
		}
	}
}
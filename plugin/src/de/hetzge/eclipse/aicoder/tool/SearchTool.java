package de.hetzge.eclipse.aicoder.tool;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.quicksearch.ProjectSearchUtils;
import de.hetzge.eclipse.aicoder.quicksearch.SearchOptions;
import de.hetzge.eclipse.aicoder.quicksearch.SearchResult;
import mjson.Json;

public final class SearchTool extends Tool {

	private static Json prepareDefinition(List<IProject> projects) {
		return Json.object()
				.set("name", "search")
				.set("description", "Searches the project for pattern in the source code")
				.set("parameters", Json.object()
						.set("type", "object")
						.set("properties", Json.object()
								.set("pattern", Json.object()
										.set("type", "string")
										.set("description", "The regex pattern to search for."))
								.set("file_pattern", Json.object()
										.set("type", "string")
										.set("description", "The regex pattern to filter by file names."))
								.set("project", Json.object()
										.set("type", "string")
										.set("description", "Project name to search in")
										.set("enum", projects.stream().map(IProject::getName).toList())))
						.set("required", Json.array().add("pattern")));
	}

	private final List<IProject> projects;
	private final FileSystem fileSystem;

	public SearchTool(List<IProject> projects, FileSystem fileSystem) {
		super(prepareDefinition(projects));
		this.projects = projects;
		this.fileSystem = fileSystem;
		if (this.projects.isEmpty()) {
			throw new IllegalArgumentException("At least one project must be provided.");
		}
	}

	@Override
	public String execute(IProgressMonitor monitor, Json arguments) {
		final String pattern = arguments.at("pattern").asString();
		final String filePattern = arguments.at("file_pattern", ".*").asString();
		final String projectName = arguments.at("project", "").asString();
		if (pattern == null || pattern.isBlank()) {
			return "Error: pattern argument is required.";
		}
		if (this.projects.stream().noneMatch(it -> it.getName().equals(projectName))) {
			return "Error: project argument is required. Available projects: " + this.projects.stream().map(IProject::getName).toList() + ".";
		}
		try {
			final StringBuilder builder = new StringBuilder();
			for (final IProject project : this.projects) {
				if (StringUtils.isNotBlank(projectName) && !project.getName().equals(projectName)) {
					continue;
				}
				final Stream<SearchResult> searchResultsA = ProjectSearchUtils.search(project, SearchOptions.builder(pattern).filePattern(filePattern).build(), new NullProgressMonitor()).stream()
						.filter(it -> !this.fileSystem.contains(it.getPath().makeRelativeTo(this.projects.get(0).getWorkspace().getRoot().getFullPath())));
				final Stream<SearchResult> searchResultsB = this.fileSystem.search(pattern, filePattern).stream();
				final List<SearchResult> results = Stream.concat(searchResultsA, searchResultsB)
						.sorted(Comparator.comparing(it -> it.getPath().toOSString()))
						.toList();
				for (final SearchResult result : results) {
					if (builder.length() > 0) {
						builder.append('\n');
					}
					builder
							.append(project.getName())
							.append(':')
							.append(result.getPath().toOSString())
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
			return builder.toString();
		} catch (final Exception exception) {
			AiCoderActivator.log().error("Error searching project", exception);
			return "Error searching project: " + exception.getMessage();
		}
	}
}
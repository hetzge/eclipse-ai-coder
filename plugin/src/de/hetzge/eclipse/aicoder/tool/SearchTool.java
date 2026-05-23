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
									.set("description", "The pattern to search for.")))
					.set("required", Json.array().add("pattern")));

	private final IProject project;

	public SearchTool(IProject project) {
		super(DEFINITION);
		this.project = project;
	}

	@Override
	public String execute(Json arguments) {
		final String pattern = arguments.at("pattern").asString();
		try {
			final List<SearchResult> results = ProjectSearchUtils.search(this.project, SearchOptions.builder(pattern).build(), new NullProgressMonitor());
			final StringBuilder builder = new StringBuilder();
			for (final SearchResult result : results) {
				if (builder.length() > 0) {
					builder.append('\n');
				}
				builder
						.append(result.getFile().getFullPath().toString())
						.append(':')
						.append(result.getLineNumber())
						.append(':')
						.append(result.getColumnStart())
						.append(':')
						.append(result.getColumnEnd())
						.append(':')
						.append(result.getLineContent());
			}
			return builder.toString();
		} catch (final Exception exception) {
			AiCoderActivator.log().error("Error searching project", exception);
			return "Error searching project: " + exception.getMessage();
		}
	}
}

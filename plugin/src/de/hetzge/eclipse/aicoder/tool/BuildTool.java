package de.hetzge.eclipse.aicoder.tool;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.compiler.IProblem;

import mjson.Json;

public final class BuildTool extends Tool {

	private static Json prepareDefinition(List<IProject> projects) {
		return Json.object()
				.set("name", "build")
				.set("type", "function") // for responses api
				.set("description", "Builds the project with the modified source files.")
				.set("parameters", Json.object()
						.set("type", "object")
						.set("properties", Json.object())
						.set("required", Json.array()));
	}

	private final List<IProject> projects;
	private final FileSystem fileSystem;

	public BuildTool(List<IProject> projects, FileSystem fileSystem) {
		super(prepareDefinition(projects));
		if (projects.isEmpty()) {
			throw new IllegalArgumentException("At least one project must be provided.");
		}
		this.projects = projects;
		this.fileSystem = fileSystem;
	}

	@Override
	public String execute(IProgressMonitor monitor, Json arguments) {
		try {
			final List<IProblem> problems = new ArrayList<>();
			for (final IProject project : this.projects) {
				final List<IProblem> newProblems = TemporaryBuildUtils.buildWithModifiedSource(monitor, project, this.fileSystem);
				problems.addAll(newProblems);
			}
			if (problems.isEmpty()) {
				return "Build successful.";
			}
			final StringBuilder result = new StringBuilder("Build completed with problems:");
			for (final IProblem problem : problems) {
				final String severity = problem.isError() ? "ERROR" : problem.isWarning() ? "WARNING" : "INFO";
				result.append('\n')
						.append(severity)
						.append(' ')
						.append(new String(problem.getOriginatingFileName()))
						.append(':')
						.append(problem.getSourceLineNumber())
						.append(": ")
						.append(problem.getMessage());
			}
			return result.toString();
		} catch (final Exception exception) {
			return "Error building project: " + exception.getMessage();
		}
	}
}
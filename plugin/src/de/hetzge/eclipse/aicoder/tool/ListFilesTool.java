package de.hetzge.eclipse.aicoder.tool;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import mjson.Json;

public final class ListFilesTool extends Tool {

	private static Json prepareDefinition(List<IProject> projects) {
		return Json.object()
				.set("name", "list_files")
				.set("description", "Lists files in the project")
				.set("parameters", Json.object()
						.set("type", "object")
						.set("properties", Json.object()
								.set("path", Json.object()
										.set("type", "string")
										.set("description", "The path to list files from (relative to workspace root, as example " + ToolUtils.createPathPrefixExamples(projects) + ")."))
								.set("file_pattern", Json.object()
										.set("type", "string")
										.set("description", "The regex pattern to search for in file names."))
								.set("max_results", Json.object()
										.set("type", "integer")
										.set("description", "The maximum number of results to return. Default: 100.")))
						.set("required", Json.array().add("path")));
	}

	private final List<IProject> projects;
	private final FileSystem fileSystem;

	public ListFilesTool(List<IProject> projects, FileSystem fileSystem) {
		super(prepareDefinition(projects));
		this.projects = projects;
		this.fileSystem = fileSystem;
		if (this.projects.isEmpty()) {
			throw new IllegalArgumentException("At least one project must be provided.");
		}
	}

	@Override
	public String execute(IProgressMonitor monitor, Json arguments) {
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
		final String pathArg = arguments.at("path", "").asString();
		final String filePatternStr = arguments.at("file_pattern", ".*").asString();
		final int maxResults = arguments.at("max_results", 100).asInteger();

		final Pattern filePattern = Pattern.compile(filePatternStr);

		// Determine the container within the project
		final IPath projectPrefix = IPath.fromOSString(project.getName());
		final IPath requestedPath = pathArg.isEmpty() ? projectPrefix : IPath.fromOSString(pathArg);
		// The path should be under the project
		if (!projectPrefix.isPrefixOf(requestedPath)) {
			return "Error: Path must be within the project " + project.getName() + ".";
		}
		final IPath relativePath = requestedPath.removeFirstSegments(projectPrefix.segmentCount());
		final IContainer container;
		if (relativePath.isEmpty()) {
			container = project;
		} else {
			final IResource resource = project.findMember(relativePath);
			if (resource == null || !resource.exists()) {
				// Check if the path exists only in fileSystem
				try {
					if (this.fileSystem.contains(requestedPath) && !this.fileSystem.readFile(requestedPath).isEmpty()) {
						// It's a file, list only that file
						if (filePattern.matcher(requestedPath.lastSegment()).find()) {
							return "[FILE] " + requestedPath.toOSString();
						} else {
							return "Error: Path does not match file pattern.";
						}
					}
				} catch (final IOException exception) {
					throw new RuntimeException("Failed to read file: " + requestedPath, exception);
				}
				return "Error: Path not found: " + pathArg;
			}
			if (!(resource instanceof IContainer)) {
				// It's a file, list only that file
				if (filePattern.matcher(resource.getName()).find()) {
					return "[FILE] " + requestedPath.toOSString();
				} else {
					return "Error: Path does not match file pattern.";
				}
			}
			container = (IContainer) resource;
		}

		// Collect resources from workspace
		final List<IResource> workspaceResources = new ArrayList<>();
		collectResources(container, filePattern, workspaceResources);

		// Collect paths from fileSystem that are not yet in workspace results
		final IPath containerPath = container.getFullPath();
		final List<IPath> fileSystemPaths = this.fileSystem.getPathsWithContent().stream()
				.filter(p -> containerPath.isPrefixOf(p) && !workspaceResources.stream()
						.anyMatch(r -> r.getFullPath().equals(p)))
				.filter(p -> filePattern.matcher(p.lastSegment()).find())
				.sorted()
				.toList();

		// Combine and limit
		final List<Object> allEntries = new ArrayList<>();
		allEntries.addAll(workspaceResources);
		allEntries.addAll(fileSystemPaths);
		allEntries.sort((a, b) -> {
			final IPath pa = (a instanceof IResource) ? ((IResource) a).getFullPath() : (IPath) a;
			final IPath pb = (b instanceof IResource) ? ((IResource) b).getFullPath() : (IPath) b;
			return pa.toPortableString().compareTo(pb.toPortableString());
		});

		final int limit = Math.min(allEntries.size(), maxResults);
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < limit; i++) {
			if (sb.length() > 0) {
				sb.append("\n");
			}
			final Object entry = allEntries.get(i);
			final String type;
			final IPath path;
			if (entry instanceof IResource) {
				final IResource resource = (IResource) entry;
				type = (resource instanceof IContainer) ? "[DIR] " : "[FILE] ";
				path = resource.getFullPath();
			} else {
				type = "[FILE] ";
				path = (IPath) entry;
			}
			sb.append(type).append(path.makeRelativeTo(project.getWorkspace().getRoot().getFullPath()));
		}
		return sb.toString();
	}

	private void collectResources(IContainer container, Pattern filePattern, List<IResource> result) {
		try {
			for (final IResource member : container.members()) {
				if (member instanceof IContainer) {
					// Add directory if it matches pattern
					if (filePattern.matcher(member.getName()).find()) {
						result.add(member);
					}
					collectResources((IContainer) member, filePattern, result);
				} else if (member instanceof IFile) {
					final IFile file = (IFile) member;
					// Skip empty files (unless they have content in fileSystem)
					boolean hasContent = false;
					try {
						hasContent = file.getLocation().toFile().length() > 0;
					} catch (final Exception e) {
						// Assume empty
					}
					if (!hasContent) {
						// Check if the fileSystem has non-empty content for this file
						final IPath filePath = file.getFullPath();
						if (!this.fileSystem.contains(filePath) || this.fileSystem.readFile(filePath).isEmpty()) {
							continue;
						}
					}
					if (filePattern.matcher(member.getName()).find()) {
						result.add(member);
					}
				}
			}
		} catch (final CoreException e) {
			// skip inaccessible containers
		} catch (final IOException exception) {
			throw new RuntimeException("Failed to read file: " + container.getFullPath(), exception);
		}
	}
}

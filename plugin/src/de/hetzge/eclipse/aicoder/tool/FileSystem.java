package de.hetzge.eclipse.aicoder.tool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import de.hetzge.eclipse.aicoder.quicksearch.SearchResult;

public final class FileSystem {

	private final List<IProject> projects;
	private final IWorkspaceRoot workspaceRoot;
	private final Map<IPath, String> contentByPath;

	public FileSystem(List<IProject> projects, IWorkspaceRoot workspaceRoot) {
		this.projects = projects;
		this.workspaceRoot = workspaceRoot;
		this.contentByPath = new HashMap<>();
	}

	// only files are moveable
	public void moveFile(IPath source, IPath destination) throws IOException {
		final String content = readFile(source);
		this.contentByPath.put(normalizePath(source), "");
		this.contentByPath.put(normalizePath(destination), content);
	}

	public void putFile(IPath path, String content) {
		this.contentByPath.put(normalizePath(path), content);
	}

	public String readFile(IPath path) throws IOException {
		path = normalizePath(path);
		if (this.contentByPath.containsKey(path)) {
			return this.contentByPath.get(path);
		} else {
			final IFile file = this.workspaceRoot.getFile(path);
			try {
				return new String(file.getContents(true).readAllBytes(), file.getCharset());
			} catch (final CoreException exception) {
				throw new IOException("Failed to read file: " + path, exception);
			}
		}
	}

	public boolean contains(IPath path) {
		return this.contentByPath.containsKey(normalizePath(path));
	}

	public List<IPath> getPathsWithContent() {
		return this.contentByPath.keySet().stream()
				.filter(it -> !this.contentByPath.get(it).isBlank())
				.toList();
	}

	public List<SearchResult> search(String pattern, String filePattern) {
		final List<SearchResult> results = new ArrayList<>();
		final Pattern regexPattern = Pattern.compile(pattern);
		final Pattern regexFilePattern = Pattern.compile(filePattern);
		for (final Map.Entry<IPath, String> entry : this.contentByPath.entrySet()) {
			if (entry.getValue().isBlank()) {
				continue;
			}
			if (!regexFilePattern.matcher(entry.getKey().toString()).matches()) {
				continue;
			}
			final String content = entry.getValue();
			try (BufferedReader reader = new BufferedReader(new StringReader(content))) {
				String line;
				int lineNumber = 0;
				while ((line = reader.readLine()) != null) {
					lineNumber++;
					final Matcher matcher = regexPattern.matcher(line);
					while (matcher.find()) {
						results.add(new SearchResult(
								entry.getKey(),
								lineNumber,
								matcher.start(),
								matcher.end(),
								line,
								matcher.group()));
					}
				}
			} catch (final IOException exception) {
				throw new IllegalStateException("Failed to read file: " + entry.getKey(), exception);
			}
		}
		return results;
	}

	private IPath normalizePath(IPath path) {
		if (isAvailableProjectPath(path)) {
			return path;
		}
		final IFile file = this.workspaceRoot.getFile(path);
		if (!file.exists()) {
			throw new IllegalArgumentException("File does not exist: " + path);
		}
		return file.getFullPath().makeRelativeTo(this.workspaceRoot.getFullPath());
	}

	private boolean isAvailableProjectPath(IPath path) {
		return this.projects.stream().anyMatch(it -> IPath.fromPortableString(it.getName()).isPrefixOf(path));
	}
}

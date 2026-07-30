package de.hetzge.eclipse.aicoder.tool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
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

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.quicksearch.SearchResult;

public final class FileSystem {

	private final List<IProject> projects;
	private final IWorkspaceRoot workspaceRoot;
	private final Map<IPath, String> referenceContentByPath;
	private final Map<IPath, String> contentByPath;

	public FileSystem(List<IProject> projects, IWorkspaceRoot workspaceRoot) {
		this.projects = projects;
		this.workspaceRoot = workspaceRoot;
		this.referenceContentByPath = new HashMap<>();
		this.contentByPath = new HashMap<>();
	}

	// only files are moveable
	public void moveFile(IPath source, IPath destination) throws IOException {
		final String content = readFile(source);
		putFile(source, "");
		putFile(destination, content);
	}

	public void putFile(IPath path, String content) throws IOException {
		if (!this.referenceContentByPath.containsKey(path)) {
			this.referenceContentByPath.put(path, readReferenceFile(path));
		}
		this.contentByPath.put(normalizePath(path), content);
	}

	public String readFile(IPath path) throws IOException {
		path = normalizePath(path);
		if (this.contentByPath.containsKey(path)) {
			return this.contentByPath.getOrDefault(path, "");
		} else {
			return readReferenceFile(path);
		}
	}

	private String readReferenceFile(IPath path) throws IOException {
		final IFile file = this.workspaceRoot.getFile(path);
		if (!file.exists()) {
			return "";
		}
		try {
			return new String(file.getContents(true).readAllBytes(), file.getCharset());
		} catch (final CoreException exception) {
			throw new IOException("Failed to read file: " + path, exception);
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

	public void persist(Path folder) throws IOException {
		AiCoderActivator.log().info("Persisting file system to " + folder);
		Files.createDirectories(folder);
		for (final Path path : Files.walk(folder).filter(Files::isRegularFile).toList()) {
			Files.delete(path);
		}
		for (final Map.Entry<IPath, String> entry : this.contentByPath.entrySet()) {
			final Path originalFilePath = folder.resolve(entry.getKey().toString()).resolveSibling(entry.getKey().toPath().getFileName() + ".original");
			final Path changedFilePath = folder.resolve(entry.getKey().toString());
			Files.createDirectories(originalFilePath.getParent());
			Files.writeString(originalFilePath, this.referenceContentByPath.getOrDefault(entry.getKey(), ""));
			Files.createDirectories(changedFilePath.getParent());
			Files.writeString(changedFilePath, entry.getValue());
		}
	}

	public void load(Path folder) throws IOException {
		AiCoderActivator.log().info("Loading file system from " + folder);
		this.contentByPath.clear();
		this.referenceContentByPath.clear();
		for (final Path path : Files.walk(folder).filter(Files::isRegularFile).toList()) {
			if (path.getFileName().toString().endsWith(".original")) {
				final IPath relativePath = IPath.fromPath(path.getParent().relativize(path)).removeFileExtension();
				this.referenceContentByPath.put(relativePath, Files.readString(path));
			} else {
				final IPath relativePath = IPath.fromPath(folder.relativize(path));
				this.contentByPath.put(relativePath, Files.readString(path));
			}
		}
	}

	private IPath normalizePath(IPath path) {
		if (isAvailableProjectPath(path)) {
			return path.makeRelative();
		}
		final IFile file = this.workspaceRoot.getFile(path);
		if (!file.exists()) {
			throw new IllegalArgumentException("File does not exist: " + path);
		}
		return file.getFullPath().makeRelativeTo(this.workspaceRoot.getFullPath()).makeRelative();
	}

	private boolean isAvailableProjectPath(IPath path) {
		return this.projects.stream().anyMatch(it -> IPath.fromPortableString(it.getName()).isPrefixOf(path));
	}

}

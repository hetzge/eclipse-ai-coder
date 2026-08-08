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
import de.hetzge.eclipse.aicoder.agent.AgentChange;
import de.hetzge.eclipse.aicoder.agent.AgentChangeType;
import de.hetzge.eclipse.aicoder.history.AiCoderHistoryEntry;
import de.hetzge.eclipse.aicoder.history.HistoryType;
import de.hetzge.eclipse.aicoder.inline.Suggestion;
import de.hetzge.eclipse.aicoder.quicksearch.SearchResult;
import de.hetzge.eclipse.aicoder.util.DiffUtils;

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

	public List<Suggestion> toSuggestions(IPath path, HistoryType historyType) throws IOException {
		final IPath normalizedPath = normalizePath(path);
		final String oldContent = this.referenceContentByPath.containsKey(normalizedPath)
				? this.referenceContentByPath.get(normalizedPath)
				: readReferenceFile(normalizedPath);
		final String newContent = this.contentByPath.getOrDefault(normalizedPath, oldContent);

		if (oldContent.equals(newContent)) {
			return List.of();
		}

		final String[] oldLines = oldContent.split("\n", -1);
		final String[] newLines = newContent.split("\n", -1);
		final int[][] lcs = new int[oldLines.length + 1][newLines.length + 1];

		for (int oldIndex = oldLines.length - 1; oldIndex >= 0; oldIndex--) {
			for (int newIndex = newLines.length - 1; newIndex >= 0; newIndex--) {
				lcs[oldIndex][newIndex] = oldLines[oldIndex].equals(newLines[newIndex])
						? lcs[oldIndex + 1][newIndex + 1] + 1
						: Math.max(lcs[oldIndex + 1][newIndex], lcs[oldIndex][newIndex + 1]);
			}
		}

		final int[] oldOffsets = new int[oldLines.length + 1];
		final int[] newOffsets = new int[newLines.length + 1];
		for (int i = 1; i < oldOffsets.length; i++) {
			final int newline = oldContent.indexOf('\n', oldOffsets[i - 1]);
			oldOffsets[i] = newline < 0 ? oldContent.length() : newline + 1;
		}
		for (int i = 1; i < newOffsets.length; i++) {
			final int newline = newContent.indexOf('\n', newOffsets[i - 1]);
			newOffsets[i] = newline < 0 ? newContent.length() : newline + 1;
		}

		final List<Suggestion> suggestions = new ArrayList<>();
		int oldIndex = 0;
		int newIndex = 0;
		int characterOffsetAdjustment = 0;
		int lineOffsetAdjustment = 0;

		while (oldIndex < oldLines.length || newIndex < newLines.length) {
			if (oldIndex < oldLines.length
					&& newIndex < newLines.length
					&& oldLines[oldIndex].equals(newLines[newIndex])) {
				oldIndex++;
				newIndex++;
				continue;
			}

			final int oldStart = oldIndex;
			final int newStart = newIndex;
			while (oldIndex < oldLines.length || newIndex < newLines.length) {
				if (oldIndex < oldLines.length
						&& newIndex < newLines.length
						&& oldLines[oldIndex].equals(newLines[newIndex])) {
					break;
				}
				if (newIndex >= newLines.length
						|| oldIndex < oldLines.length && lcs[oldIndex + 1][newIndex] >= lcs[oldIndex][newIndex + 1]) {
					oldIndex++;
				} else {
					newIndex++;
				}
			}

			final int oldLength = oldOffsets[oldIndex] - oldOffsets[oldStart];
			final String content = newContent.substring(newOffsets[newStart], newOffsets[newIndex]);
			final int oldLineCount = oldIndex - oldStart;
			final int newLineCount = newIndex - newStart;

			System.out.println(" ------------------> " + (oldOffsets[oldStart] + characterOffsetAdjustment));

			suggestions.add(new Suggestion(
					new AiCoderHistoryEntry(historyType, normalizedPath.toString(), oldContent),
					content,
					oldOffsets[oldStart] + characterOffsetAdjustment,
					oldLength,
					Math.max(0, oldStart + oldLineCount - 1 + lineOffsetAdjustment),
					newLineCount,
					oldLineCount));

			characterOffsetAdjustment += content.length() - oldLength;
			lineOffsetAdjustment += newLineCount - oldLineCount;
		}
		return suggestions;
	}

	public List<AgentChange> toAgentChanges() {
		final List<AgentChange> changes = new ArrayList<>();
		for (final Map.Entry<IPath, String> entry : this.contentByPath.entrySet()) {
			final String oldContent = this.referenceContentByPath.getOrDefault(entry.getKey(), "");
			final String newContent = entry.getValue();
			final DiffUtils.Diff diff = DiffUtils.diff(oldContent, newContent);
			if (entry.getValue().isBlank()) {
				changes.add(new AgentChange(entry.getKey(), AgentChangeType.DELETED, 0, diff.removed()));
			} else if (this.referenceContentByPath.containsKey(entry.getKey())) {
				changes.add(new AgentChange(entry.getKey(), AgentChangeType.MODIFIED, diff.added(), diff.removed()));
			} else {
				changes.add(new AgentChange(entry.getKey(), AgentChangeType.CREATED, diff.added(), 0));
			}
		}
		return changes;
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

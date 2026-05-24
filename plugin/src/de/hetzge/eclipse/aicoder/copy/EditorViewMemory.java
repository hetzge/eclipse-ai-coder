package de.hetzge.eclipse.aicoder.copy;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.ITextEditor;

import de.hetzge.eclipse.aicoder.util.EclipseUtils;

/**
 * Maintains a memory of editor viewports with merging and line limit functionality.
 */
public final class EditorViewMemory {

	private static final int DEFAULT_MAX_LINES = 10000;

	// Stores lines for each file, indexed by line number
	private final Map<String, SortedMap<Integer, String>> fileToLines;

	// Tracks insertion order for eviction when memory is full
	private final Deque<EditorView> insertionOrder;

	// Maximum number of lines to store
	private final int maxLines;

	// Current total line count
	private int totalLineCount;

	/**
	 * Creates EditorViewMemory with default limit of 1000 lines
	 */
	public EditorViewMemory() {
		this(DEFAULT_MAX_LINES);
	}

	/**
	 * Creates EditorViewMemory with specified line limit
	 *
	 * @param maxLines maximum number of lines to store
	 */
	public EditorViewMemory(int maxLines) {
		if (maxLines <= 0) {
			throw new IllegalArgumentException("maxLines must be positive");
		}
		this.maxLines = maxLines;
		this.fileToLines = new HashMap<>();
		this.insertionOrder = new ArrayDeque<>();
		this.totalLineCount = 0;
	}

	public void update() {
		final Optional<AbstractTextEditor> activeTextEditorOptional = EclipseUtils.getActiveTextEditor();
		if (!activeTextEditorOptional.isPresent()) {
			return;
		}
		final AbstractTextEditor textEditor = activeTextEditorOptional.get();
		update(textEditor);
	}

	/**
	 * Updates the memory with the current editor viewport
	 */
	public void update(ITextEditor textEditor) {
		if (textEditor == null) {
			return;
		}
		final Optional<EditorView> editorViewOptional = EditorView.createFromTextEditor(textEditor);
		if (!editorViewOptional.isPresent()) {
			return;
		}
		update(editorViewOptional.get());
	}

	/**
	 * Updates the memory with a viewport record
	 *
	 * @param view the viewport record containing path, lineIndex, and lines
	 */
	public void update(EditorView view) {
		if (view.lines().isEmpty()) {
			return;
		}

		AiCoderActivator.log().info("Updating editor view memory for file: %s with %d lines".formatted(view.path(), view.lines().size()));

		// Add the viewport to insertion order for tracking
		this.insertionOrder.addLast(view);

		// Get or create the sorted map for this file
		final SortedMap<Integer, String> linesMap = this.fileToLines
				.computeIfAbsent(view.path(), k -> new TreeMap<>());

		// Add all lines from the new viewport ( TreeMap automatically handles merging)
		int newLinesAdded = 0;
		for (int i = 0; i < view.lines().size(); i++) {
			final int lineNum = view.lineIndex() + i;
			final String existingLine = linesMap.get(lineNum);
			final String newLine = view.lines().get(i);

			if (existingLine == null) {
				linesMap.put(lineNum, newLine);
				newLinesAdded++;
			} else if (!existingLine.equals(newLine)) {
				// Update existing line if content changed
				linesMap.put(lineNum, newLine);
			}
		}

		this.totalLineCount += newLinesAdded;

		// Evict old viewports if we exceed the limit
		evictIfNecessary();
	}

	/**
	 * Removes oldest viewport records if memory exceeds limit
	 */
	private void evictIfNecessary() {
		while (this.totalLineCount > this.maxLines && !this.insertionOrder.isEmpty()) {
			final EditorView oldest = this.insertionOrder.removeFirst();
			removeViewport(oldest);
		}
	}

	/**
	 * Removes a viewport from the memory
	 *
	 * @param record the viewport to remove
	 */
	private void removeViewport(EditorView record) {
		final SortedMap<Integer, String> linesMap = this.fileToLines.get(record.path());
		if (linesMap == null) {
			return;
		}

		// Find and remove lines that belong only to this viewport
		// A line is removed only if its exact index and content match the viewport
		final List<Integer> keysToRemove = new ArrayList<>();

		for (int i = 0; i < record.lines().size(); i++) {
			final int lineNum = record.lineIndex() + i;
			final String content = record.lines().get(i);

			if (content.equals(linesMap.get(lineNum))) {
				keysToRemove.add(lineNum);
			}
		}

		for (final Integer key : keysToRemove) {
			linesMap.remove(key);
			this.totalLineCount--;
		}

		// Clean up empty file entries
		if (linesMap.isEmpty()) {
			this.fileToLines.remove(record.path());
		}
	}

	/**
	 * Generates a report of all stored content organized by file
	 *
	 * @param excludePathString path to exclude from report
	 * @param excludeFirstLine  first line to exclude (starts with 0)
	 * @param excludeLastLine   last line to exclude (starts with 0)
	 * @param maxLines          maximum number of lines to include
	 * @return formatted string showing all remembered code
	 */
	public String getReport(String excludePathString, int excludeFirstLine, int excludeLastLine, int maxLines) {

		// Remove files that no longer exist
		for (final String path : new ArrayList<>(this.fileToLines.keySet())) {
			final IPath eclipsePath = new Path(path);
			final IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
			final IFile file = workspaceRoot.getFile(eclipsePath);
			if (!file.exists()) {
				this.fileToLines.remove(path);
			}
		}

		final StringBuilder stringBuilder = new StringBuilder();
		final List<String> sortedFiles = new ArrayList<>(this.fileToLines.keySet());
		Collections.sort(sortedFiles);
		int writtenLines = 0;
		for (int i = 0; i < sortedFiles.size(); i++) {
			if (writtenLines >= maxLines) {
				break;
			}
			final String filePath = sortedFiles.get(i);
			final SortedMap<Integer, String> linesMap = this.fileToLines.get(filePath);
			if (linesMap.isEmpty()) {
				continue;
			}
			final int minLineNumber = linesMap.keySet().stream().min(Integer::compare).orElse(0);
			final int maxLineNumber = linesMap.keySet().stream().max(Integer::compare).orElse(0);
			if (filePath.equals(excludePathString) && minLineNumber >= excludeFirstLine && maxLineNumber <= excludeLastLine) {
				AiCoderActivator.log().info("Skipping file: %s because all lines are excluded".formatted(filePath));
				continue;
			}
			stringBuilder.append("File: ").append(filePath).append("\n");
			writtenLines++;
			if (writtenLines >= maxLines) {
				break;
			}
			for (final Entry<Integer, String> lineEntry : linesMap.entrySet()) {
				if (writtenLines >= maxLines) {
					break;
				}
				// Exclude the specified range (this is typically the fill in middle context to prevent duplication and confusion)
				final boolean isExcludePath = filePath.equals(excludePathString);
				if (isExcludePath && lineEntry.getKey() == excludeFirstLine) {
					stringBuilder.append("...\n");
					writtenLines++;
					if (writtenLines >= maxLines) {
						break;
					}
				}
				if (isExcludePath && lineEntry.getKey() >= excludeFirstLine && lineEntry.getKey() <= excludeLastLine) {
					continue;
				}
				stringBuilder.append(lineEntry.getValue());
				writtenLines++;
			}
			if (i < sortedFiles.size() - 1 && writtenLines < maxLines) {
				stringBuilder.append("\n---\n");
				writtenLines++;
			}
		}
		return stringBuilder.toString();
	}

	/**
	 * Returns the content for a specific file
	 *
	 * @param path the file path
	 * @return map of line numbers to content, or empty map if file not found
	 */
	public Map<Integer, String> getFileContent(String path) {
		final SortedMap<Integer, String> linesMap = this.fileToLines.get(path);
		if (linesMap == null) {
			return Collections.emptyMap();
		}
		return Collections.unmodifiableMap(linesMap);
	}

	/**
	 * Returns the total number of lines currently stored
	 *
	 * @return line count
	 */
	public int getLineCount() {
		return this.totalLineCount;
	}

	/**
	 * Returns the maximum line limit
	 *
	 * @return max lines
	 */
	public int getMaxLines() {
		return this.maxLines;
	}

	/**
	 * Clears all stored content
	 */
	public void clear() {
		this.fileToLines.clear();
		this.insertionOrder.clear();
		this.totalLineCount = 0;
	}
}
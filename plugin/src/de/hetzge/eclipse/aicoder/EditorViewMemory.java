package de.hetzge.eclipse.aicoder;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.ui.texteditor.AbstractTextEditor;

import de.hetzge.eclipse.aicoder.util.EclipseUtils;

/**
 * Maintains a memory of editor viewports with merging and line limit functionality.
 */
public final class EditorViewMemory {

	private static final int DEFAULT_MAX_LINES = 1000;

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
		final ITextViewer textViewer = EclipseUtils.getTextViewer(activeTextEditorOptional.get());
		update(textViewer);
	}

	/**
	 * Updates the memory with the current editor viewport
	 */
	public void update(ITextViewer textViewer) {
		if (textViewer == null) {
			return;
		}
		final Optional<EditorView> editorViewOptional = EditorView.createFromTextEditor(textViewer);
		if (!editorViewOptional.isPresent()) {
			return;
		}
		update(editorViewOptional.get());
	}

	/**
	 * Updates the memory with a viewport record
	 *
	 * @param record the viewport record containing path, lineIndex, and lines
	 */
	public void update(EditorView record) {
		if (record.lines().isEmpty()) {
			return;
		}

		AiCoderActivator.log().info("Updating editor view memory for file: %s with %d lines".formatted(record.path(), record.lines().size()));

		// Add the viewport to insertion order for tracking
		this.insertionOrder.addLast(record);

		// Get or create the sorted map for this file
		final SortedMap<Integer, String> linesMap = this.fileToLines
				.computeIfAbsent(record.path(), k -> new TreeMap<>());

		// Add all lines from the new viewport ( TreeMap automatically handles merging)
		int newLinesAdded = 0;
		for (int i = 0; i < record.lines().size(); i++) {
			final int lineNum = record.lineIndex() + i;
			final String existingLine = linesMap.get(lineNum);
			final String newLine = record.lines().get(i);

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
	 * @return formatted string showing all remembered code
	 */
	public String getReport() {
		final StringBuilder stringBuilder = new StringBuilder();
		final List<String> sortedFiles = new ArrayList<>(this.fileToLines.keySet());
		Collections.sort(sortedFiles);
		for (int i = 0; i < sortedFiles.size(); i++) {
			final String filePath = sortedFiles.get(i);
			stringBuilder.append("File: ").append(filePath).append("\n");
			final SortedMap<Integer, String> linesMap = this.fileToLines.get(filePath);
			for (final String line : linesMap.values()) {
				stringBuilder.append(line);
			}
			if (i < sortedFiles.size() - 1) {
				stringBuilder.append("\n---\n");
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
package de.hetzge.eclipse.aicoder.context;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.internal.texteditor.EditPosition;
import org.eclipse.ui.internal.texteditor.TextEditorPlugin;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.AiCoderImageKey;
import de.hetzge.eclipse.aicoder.util.ContextUtils;
import de.hetzge.eclipse.aicoder.util.EclipseUtils;
import de.hetzge.eclipse.aicoder.util.LambdaExceptionUtils;

public class LastEditsContextEntry extends ContextEntry {

	public static final String PREFIX = "LAST_EDITS";
	private static final int CONTEXT_PADDING_LINE_COUNT = 10; // TODO preferences

	private final List<CodeLocation> lastCodeLocations;

	private LastEditsContextEntry(List<CodeLocation> lastCodeLocations, Duration creationDuration) {
		super(List.of(), creationDuration);
		this.lastCodeLocations = lastCodeLocations;
	}

	@Override
	public String getLabel() {
		return "Last Edits";
	}

	@Override
	public ContextEntryKey getKey() {
		return new ContextEntryKey(PREFIX, PREFIX);
	}

	@Override
	public Image getImage() {
		return AiCoderActivator.getImage(AiCoderImageKey.BEFORE_ICON);
	}

	@Override
	public String getContent(ContextContext context) {
		return ContextUtils.contentTemplate("Last edit locations", this.lastCodeLocations.stream()
				.map(snippet -> ContextUtils.codeTemplate(snippet.name, snippet.content))
				.collect(Collectors.joining("\n")));
	}

	public static ContextEntryFactory factory() {
		return new ContextEntryFactory(PREFIX, () -> create());
	}

	@SuppressWarnings("restriction")
	public static LastEditsContextEntry create() throws CoreException {
		final long before = System.currentTimeMillis();
		final List<CodeLocation> codeLocations = new ArrayList<>();
		try {
			for (final EditPosition position : getLastEditPositions()) {
				if (codeLocations.size() > 10) { // TODO preferences
					break;
				}
				final IEditorInput input = position.getEditorInput();
				if (isNearCurrentEditLocation(input)) {
					continue;
				}
				final int offset = position.getPosition().getOffset();
				if (offset < 0) {
					continue;
				}
				final IDocument document = Display.getDefault().syncCall(() -> EclipseUtils.getDocumentForEditor(input)).orElse(null);
				if (document == null) {
					continue;
				}
				final int line = document.getLineOfOffset(offset);
				final int startLine = Math.max(0, line - CONTEXT_PADDING_LINE_COUNT);
				final int endLine = Math.min(document.getNumberOfLines() - 1, line + CONTEXT_PADDING_LINE_COUNT);
				final int startOffset = document.getLineOffset(startLine);
				final int endOffset = endLine + 1 < document.getNumberOfLines() ? document.getLineOffset(endLine + 1) - 1 : document.getLength() - 1;
				final String content = document.get(startOffset, endOffset - startOffset);
				final CodeLocation codeLocation = new CodeLocation(input.getName(), startLine, endLine, content);
				// Merge overlapping code locations
				codeLocations.removeAll(codeLocations.stream().filter(existing -> existing.doesOverlap(codeLocation)).toList());
				codeLocations.add(codeLocations.stream().filter(existing -> existing.doesOverlap(codeLocation)).toList().stream().reduce(codeLocation, CodeLocation::merge));
			}
			return new LastEditsContextEntry(codeLocations, Duration.ofMillis(System.currentTimeMillis() - before));
		} catch (final BadLocationException exception) {
			throw new CoreException(new Status(IStatus.ERROR, AiCoderActivator.PLUGIN_ID, "Failed to create last edit context entry", exception));
		}
	}

	private static boolean isNearCurrentEditLocation(IEditorInput input) throws BadLocationException {
		return EclipseUtils.getActiveEditor().stream().anyMatch(editor -> Objects.equals(editor.getEditorInput(), input)) && EclipseUtils.getActiveTextEditor().map(LambdaExceptionUtils.rethrowFunction(editor -> {
			final int currentOffset = EclipseUtils.getCurrentOffsetInDocument(editor);
			final int currentLine = EclipseUtils.getDocumentForEditor(editor.getEditorInput()).map(LambdaExceptionUtils.rethrowFunction(document -> document.getLineOfOffset(currentOffset))).orElse(0);
			final int line = EclipseUtils.getDocumentForEditor(input).map(LambdaExceptionUtils.rethrowFunction(document -> document.getLineOfOffset(currentOffset))).orElse(0);
			return Math.abs(line - currentLine) < CONTEXT_PADDING_LINE_COUNT + 2;
		})).orElse(false);
	}

	@SuppressWarnings("restriction") // Accessing internal is easier then cloning all the logic for now
	private static List<EditPosition> getLastEditPositions() {
		return TextEditorPlugin.getDefault().getEditPositionHistory().rawHistory().toList().reversed().stream().limit(3).toList();
	}

	private static record CodeLocation(String name, int firstLine, int lastLine, String content) {
		public boolean doesOverlap(CodeLocation other) {
			return this.name.equals(other.name) && this.lastLine >= other.firstLine && this.firstLine <= other.lastLine;
		}

		public CodeLocation merge(CodeLocation other) {
			return new CodeLocation(this.name, Math.min(this.firstLine, other.firstLine), Math.max(this.lastLine, other.lastLine), mergeContent(this, other));
		}

		private static String mergeContent(CodeLocation locationA, CodeLocation locationB) {
			final String[] linesA = locationA.content.split("\n");
			final String[] linesB = locationB.content.split("\n");
			final int firstLine = Math.min(locationA.firstLine, locationB.firstLine);
			final int lastLine = Math.max(locationA.lastLine, locationB.lastLine);
			final int expectedLineCountA = locationA.lastLine - locationA.firstLine + 1;
			if (linesA.length != expectedLineCountA) {
				throw new IllegalStateException(String.format("Invalid line count %s but expected %s for location A", linesA.length, expectedLineCountA));
			}
			final int expcetedLineCountB = locationB.lastLine - locationB.firstLine + 1;
			if (linesB.length != expcetedLineCountB) {
				throw new IllegalStateException(String.format("Invalid line count %s but expected %s for location B", linesB.length, expcetedLineCountB));
			}
			final String[] mergedLines = new String[lastLine - firstLine + 1];
			for (int i = 0; i < mergedLines.length; i++) {
				final int lineIndex = Math.min(locationA.firstLine, locationB.firstLine) + i;
				if (lineIndex >= locationA.firstLine && lineIndex <= locationA.lastLine) {
					mergedLines[i] = linesA[i - (locationA.firstLine - firstLine)];
				}
				if (lineIndex >= locationB.firstLine && lineIndex <= locationB.lastLine) {
					mergedLines[i] = linesB[i - (locationB.firstLine - firstLine)];
				}
			}
			return String.join("\n", mergedLines);
		}
	}
}
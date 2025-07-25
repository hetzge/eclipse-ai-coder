package de.hetzge.eclipse.aicoder.context;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.INavigationHistory;
import org.eclipse.ui.INavigationLocation;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.texteditor.EditPosition;
import org.eclipse.ui.internal.texteditor.TextEditorPlugin;
import org.eclipse.ui.texteditor.TextSelectionNavigationLocation;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.AiCoderImageKey;
import de.hetzge.eclipse.aicoder.util.EclipseUtils;

public class LastEditContextEntry extends ContextEntry {

	public static final String PREFIX = "LAST_EDIT";

	private final List<CodeLocation> lastCodeLocations;

	private LastEditContextEntry(List<CodeLocation> lastCodeLocations, Duration creationDuration) {
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
	public String getContent(ContextContext context) {
		return "Last edit locations:\n" + this.lastCodeLocations.stream()
				.map(snippet -> String.format("Edit location: %s\n---\n%s\n---\n", snippet.name, snippet.content))
				.collect(Collectors.joining("\n"));
	}

	@Override
	public org.eclipse.swt.graphics.Image getImage() {
		return AiCoderActivator.getImage(AiCoderImageKey.BEFORE_ICON);
	}

	@SuppressWarnings("restriction")
	public static LastEditContextEntry create() throws CoreException {
		final long before = System.currentTimeMillis();
//		final Optional<INavigationHistory> navigationHistoryOptional = getNavigationHistory();
//		if (!navigationHistoryOptional.isPresent()) {
//			return new LastEditContextEntry(List.of(), Duration.ofMillis(System.currentTimeMillis() - before));
//		}
//		final INavigationHistory navigationHistory = navigationHistoryOptional.get();
//		final INavigationLocation[] locations = navigationHistory.getLocations();
		// TODO ignore near current location
		final List<CodeLocation> codeLocations = new ArrayList<>();
		for (final EditPosition position : getLastEditPositions()) {
			try {
				if (codeLocations.size() > 10) {
					break;
				}
				final IEditorInput input = position.getEditorInput();
				final String name = input.getName();
				final int offset = position.getPosition().getOffset();
				if (offset < 0) {
					continue;
				}
				final IDocument document = Display.getDefault().syncCall(() -> EclipseUtils.getDocumentForEditor(input));
				if (document == null) {
					continue;
				}
				final int line = document.getLineOfOffset(offset);
				final int startLine = Math.max(0, line - 10);
				final int endLine = Math.min(document.getNumberOfLines() - 1, line + 10);
				final int startOffset = document.getLineOffset(startLine);
				final int endOffset = endLine + 1 < document.getNumberOfLines() ? document.getLineOffset(endLine + 1) - 1 : document.getLength() - 1;
				final String content = document.get(startOffset, endOffset - startOffset);
				final CodeLocation codeLocation = new CodeLocation(name, startLine, endLine, content);
				// Merge overlapping code locations
				codeLocations.removeAll(codeLocations.stream().filter(existing -> existing.doesOverlap(codeLocation)).toList());
				codeLocations.add(codeLocations.stream().filter(existing -> existing.doesOverlap(codeLocation)).toList().stream().reduce(codeLocation, CodeLocation::merge));
			} catch (final BadLocationException exception) {
				throw new CoreException(new Status(IStatus.ERROR, AiCoderActivator.PLUGIN_ID, "Failed to create last edit context entry", exception));
			}
		}
		return new LastEditContextEntry(codeLocations, Duration.ofMillis(System.currentTimeMillis() - before));
	}

	@SuppressWarnings("restriction") // Accessing internal is easier then cloning all the logic for now
	private static List<EditPosition> getLastEditPositions() {
		return TextEditorPlugin.getDefault().getEditPositionHistory().rawHistory().toList().reversed().stream().limit(3).toList();
	}

	private static Optional<INavigationHistory> getNavigationHistory() {
		return Display.getDefault().syncCall(() -> {
			return Optional.of(PlatformUI.getWorkbench())
					.map(IWorkbench::getActiveWorkbenchWindow)
					.map(IWorkbenchWindow::getActivePage)
					.map(IWorkbenchPage::getNavigationHistory);
		});
	}

	private static int getOffsetFromLocation(INavigationLocation location) throws CoreException {
		try {
			// Using reflection here as workaround for missing API to get the offset of a navigation location
			final Field fPositionField = TextSelectionNavigationLocation.class.getDeclaredField("fPosition");
			fPositionField.setAccessible(true);
			final Position position = (Position) fPositionField.get(location);
			return position.getOffset();
		} catch (NoSuchFieldException | IllegalAccessException exception) {
			throw new CoreException(new Status(IStatus.ERROR, AiCoderActivator.PLUGIN_ID, "Failed to get offset from navigation location", exception));
		}
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
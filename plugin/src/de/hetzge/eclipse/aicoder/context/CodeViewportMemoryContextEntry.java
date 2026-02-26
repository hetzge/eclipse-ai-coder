package de.hetzge.eclipse.aicoder.context;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.graphics.Image;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.AiCoderImageKey;
import de.hetzge.eclipse.aicoder.EditorView;
import de.hetzge.eclipse.aicoder.util.ContextUtils;

// TODO exclude fill in middle context
// TODO filter by current file ending

public final class CodeViewportMemoryContextEntry extends ContextEntry {

	public static final String LABEL = "Code Viewport Memory";
	public static final String PREFIX = "CODE_VIEWPORT_MEMORY";

	private final String pathString;
	private final int firstLine;
	private final int lastLine;

	private CodeViewportMemoryContextEntry(List<? extends ContextEntry> childContextEntries, Duration creationDuration, String pathString, int firstLine, int lastLine) {
		super(childContextEntries, creationDuration);
		this.pathString = pathString;
		this.firstLine = firstLine;
		this.lastLine = lastLine;
	}

	@Override
	public ContextEntryKey getKey() {
		return new ContextEntryKey(PREFIX, PREFIX);
	}

	@Override
	public String getLabel() {
		return "Code Viewport Memory";
	}

	@Override
	public Image getImage() {
		return AiCoderActivator.getImage(AiCoderImageKey.VIEWPORT_ICON);
	}

	@Override
	public String getContent(ContextContext context) {
		final String report = AiCoderActivator.getDefault().getEditorViewMemory().getReport(this.pathString, this.firstLine, this.lastLine);
		return ContextUtils.contentTemplate("Code Viewport Memory", report != null ? report : "No report available");
	}

	@Override
	public List<? extends ContextEntry> getChildContextEntries() {
		return Collections.emptyList();
	}

	public static ContextEntryFactory factory(IDocument document, int modelOffset) {
		return new ContextEntryFactory(PREFIX,
				() -> create(document, modelOffset),
				() -> new EmptyContextEntry(PREFIX, LABEL, null));
	}

	public static CodeViewportMemoryContextEntry create(IDocument document, int modelOffset) throws CoreException {
		try {
			final long before = System.currentTimeMillis();
			final String pathString = EditorView.getPathString(document);
			final int firstLine = FillInMiddleContextEntry.calculateFirstLine(document, modelOffset);
			final int lastLine = FillInMiddleContextEntry.calculateLastLine(document, modelOffset);
			return new CodeViewportMemoryContextEntry(
					Collections.emptyList(),
					Duration.ofMillis(System.currentTimeMillis() - before),
					pathString,
					firstLine,
					lastLine);
		} catch (final BadLocationException exception) {
			throw new CoreException(Status.error("Failed to create CodeViewportMemoryContextEntry", exception));
		}
	}
}
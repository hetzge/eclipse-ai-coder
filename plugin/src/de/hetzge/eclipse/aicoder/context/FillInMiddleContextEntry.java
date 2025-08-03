package de.hetzge.eclipse.aicoder.context;

import java.time.Duration;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.graphics.Image;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.AiCoderImageKey;
import de.hetzge.eclipse.aicoder.preferences.AiCoderPreferences;
import de.hetzge.eclipse.aicoder.util.ContextUtils;

public class FillInMiddleContextEntry extends ContextEntry {
	public static final String FILL_HERE_PLACEHOLDER = "<<<<FILL_HERE>>>>";
	public static final String PREFIX = "FILL_IN_MIDDLE";

	private final String filename;
	private final String prefix;
	private final String suffix;

	private FillInMiddleContextEntry(String filename, String prefix, String suffix, Duration creationDuration) {
		super(List.of(), creationDuration);
		this.filename = filename;
		this.prefix = prefix;
		this.suffix = suffix;
	}

	@Override
	public ContextEntryKey getKey() {
		return new ContextEntryKey(PREFIX, PREFIX);
	}

	@Override
	public String getLabel() {
		return "Fill in the middle";
	}

	@Override
	public Image getImage() {
		return AiCoderActivator.getImage(AiCoderImageKey.FILL_IN_MIDDLE_ICON);
	}

	@Override
	public String getContent(ContextContext context) {
		return ContextUtils.contentTemplate(String.format("Current edit location: %s", this.filename), this.prefix + FILL_HERE_PLACEHOLDER + this.suffix);
	}

	public static FillInMiddleContextEntry create(String filename, IDocument document, int modelOffset) throws BadLocationException {
		final long before = System.currentTimeMillis();
		final String prefix = getPrefix(document, modelOffset);
		final String suffix = getSuffix(document, modelOffset);
		return new FillInMiddleContextEntry(filename, prefix, suffix, Duration.ofMillis(System.currentTimeMillis() - before));
	}

	private static String getPrefix(IDocument document, int modelOffset) throws BadLocationException {
		final int modelLine = document.getLineOfOffset(modelOffset);
		final int maxLines = AiCoderPreferences.getMaxPrefixSize();
		final int firstLine = Math.max(0, modelLine - maxLines);
		return document.get(document.getLineOffset(firstLine), modelOffset - document.getLineOffset(firstLine));
	}

	private static String getSuffix(IDocument document, int modelOffset) throws BadLocationException {
		final int modelLine = document.getLineOfOffset(modelOffset);
		final int maxLines = AiCoderPreferences.getMaxSuffixSize();
		final int lastLine = Math.max(document.getNumberOfLines() - 1, modelLine + maxLines);
		return document.get(modelOffset, lastLine >= document.getNumberOfLines() ? document.getLength() - modelOffset : document.getLineOffset(lastLine) - modelOffset);
	}
}

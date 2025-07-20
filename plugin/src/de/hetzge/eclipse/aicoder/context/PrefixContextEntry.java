package de.hetzge.eclipse.aicoder.context;

import java.time.Duration;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.graphics.Image;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.AiCoderImageKey;
import de.hetzge.eclipse.aicoder.preferences.AiCoderPreferences;

public class PrefixContextEntry extends ContextEntry {
	public static final String PREFIX = "PREFIX";

	private final String filename;
	private final String content;

	private PrefixContextEntry(String filename, String content, Duration creationDuration) {
		super(List.of(), creationDuration);
		this.filename = filename;
		this.content = content;
	}

	@Override
	public ContextEntryKey getKey() {
		return new ContextEntryKey(PREFIX, PREFIX);
	}

	@Override
	public String getLabel() {
		return "Prefix";
	}

	@Override
	public Image getImage() {
		return AiCoderActivator.getImage(AiCoderImageKey.BEFORE_ICON);
	}

	@Override
	public String getContent(ContextContext context) {
		return String.format("File: %s\n%s", this.filename, this.content);
	}

	public static PrefixContextEntry create(String filename, IDocument document, int modelOffset) throws BadLocationException {
		final long before = System.currentTimeMillis();
		final int modelLine = document.getLineOfOffset(modelOffset);
		final int maxLines = AiCoderPreferences.getMaxPrefixSize();
		final int firstLine = Math.max(0, modelLine - maxLines);
		final String prefix = document.get(document.getLineOffset(firstLine), modelOffset - document.getLineOffset(firstLine));
		return new PrefixContextEntry(filename, prefix, Duration.ofMillis(System.currentTimeMillis() - before));
	}
}
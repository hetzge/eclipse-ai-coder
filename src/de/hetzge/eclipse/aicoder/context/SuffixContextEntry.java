package de.hetzge.eclipse.aicoder.context;

import java.time.Duration;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.graphics.Image;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.AiCoderImageKey;

public class SuffixContextEntry extends ContextEntry {
	public static final String FILL_HERE_PLACEHOLDER = "<<FILL_HERE>>";
	private final String content;

	public static final String PREFIX = "SUFFIX";

	public SuffixContextEntry(String content, Duration creationDuration) {
		super(List.of(), creationDuration);
		this.content = content;
	}

	@Override
	public ContextEntryKey getKey() {
		return new ContextEntryKey(PREFIX, PREFIX);
	}

	@Override
	public String getLabel() {
		return "Suffix";
	}

	@Override
	public Image getImage() {
		return AiCoderActivator.getImage(AiCoderImageKey.AFTER_ICON);
	}

	@Override
	public String getContent(ContextContext context) {
		return FILL_HERE_PLACEHOLDER + this.content + "\n";
	}

	public static SuffixContextEntry create(IDocument document, int modelOffset) throws BadLocationException {
		final long before = System.currentTimeMillis();
		final String suffix = document.get(modelOffset, document.getLength() - modelOffset);
		return new SuffixContextEntry(suffix, Duration.ofMillis(System.currentTimeMillis() - before));
	}
}
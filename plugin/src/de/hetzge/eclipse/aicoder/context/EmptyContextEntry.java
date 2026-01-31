package de.hetzge.eclipse.aicoder.context;

import java.time.Duration;
import java.util.List;

import org.eclipse.swt.graphics.Image;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.AiCoderImageKey;

public class EmptyContextEntry extends ContextEntry {

	public static final String PREFIX = "EMPTY";

	private final String prefix;
	private final String label;
	private final AiCoderImageKey imageKey;

	public EmptyContextEntry() {
		this(PREFIX, "Empty", null);
	}

	public EmptyContextEntry(String prefix, String label, AiCoderImageKey imageKey) {
		super(List.of(), Duration.ZERO);
		this.prefix = prefix;
		this.label = label;
		this.imageKey = imageKey;
	}

	@Override
	public String getLabel() {
		return this.label;
	}

	@Override
	public Image getImage() {
		return this.imageKey == null ? super.getImage() : AiCoderActivator.getImage(this.imageKey);
	}

	@Override
	public ContextEntryKey getKey() {
		return new ContextEntryKey(this.prefix, "EMPTY");
	}

}
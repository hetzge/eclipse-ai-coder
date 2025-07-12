package de.hetzge.eclipse.aicoder.context;

import java.time.Duration;
import java.util.List;

public class EmptyContextEntry extends ContextEntry {

	public static final String PREFIX = "EMPTY";

	public EmptyContextEntry() {
		super(List.of(), Duration.ZERO);
	}

	@Override
	public String getLabel() {
		return "Empty";
	}

	@Override
	public ContextEntryKey getKey() {
		return new ContextEntryKey(PREFIX, PREFIX);
	}
}
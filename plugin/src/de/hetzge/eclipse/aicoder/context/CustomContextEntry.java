package de.hetzge.eclipse.aicoder.context;

import java.time.Duration;
import java.util.List;

public class CustomContextEntry extends ContextEntry {
	public static final String PREFIX = "CUSTOM";

	private final CustomContextEntryData data;
	private final boolean active;

	public CustomContextEntry(CustomContextEntryData data, boolean active) {
		super(List.of(), Duration.ZERO);
		this.data = data;
		this.active = active;
	}

	@Override
	public ContextEntryKey getKey() {
		return new ContextEntryKey(PREFIX, this.data.getId().toString());
	}

	@Override
	public String getLabel() {
		return "%s %s".formatted(this.active ? "✅" : "❌", this.data.getTitle());
	}

	@Override
	public String getContent(ContextContext context) {
		if (!this.active) {
			return "";
		}
		return String.format("%s\n%s", this.data.getContent(), super.getContent(context));
	}

	public CustomContextEntryData getData() {
		return this.data;
	}

	public boolean isActive() {
		return this.active;
	}
}

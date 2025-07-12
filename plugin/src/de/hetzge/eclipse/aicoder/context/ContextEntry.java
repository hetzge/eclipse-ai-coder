package de.hetzge.eclipse.aicoder.context;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import de.hetzge.eclipse.aicoder.ContextPreferences;

public abstract class ContextEntry {

	private int tokenCount;
	protected final List<? extends ContextEntry> childContextEntries;
	private final Duration creationDuration;

	public ContextEntry(List<? extends ContextEntry> childContextEntries, Duration creationDuration) {
		this.tokenCount = 0;
		this.childContextEntries = childContextEntries;
		this.creationDuration = creationDuration;
	}

	public int getTokenCount() {
		return this.tokenCount;
	}

	public void setTokenCount(int tokenCount) {
		this.tokenCount = tokenCount;
	}

	public abstract ContextEntryKey getKey();

	public String getContent(ContextContext context) {
		return this.childContextEntries.stream()
				.map(entry -> apply(entry, context))
				// not using "\n" here because prefix/suffix should not be separated by line break
				.collect(Collectors.joining());
	}

	public List<? extends ContextEntry> getChildContextEntries() {
		return this.childContextEntries;
	}

	public abstract String getLabel();

	public Image getImage() {
		return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_ELEMENT);
	}

	public Duration getCreationDuration() {
		return this.creationDuration;
	}

	public static String apply(final ContextEntry entry, ContextContext context) {
		if (ContextPreferences.isBlacklisted(entry.getKey()) || context.isDone(entry)) {
			entry.setTokenCount(0);
			return "";
		}
		context.markDone(entry);
		final String content = entry.getContent(context);
		entry.setTokenCount(content.length());
		return content;
	}
}
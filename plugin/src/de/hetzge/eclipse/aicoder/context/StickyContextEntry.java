package de.hetzge.eclipse.aicoder.context;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.graphics.Image;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.AiCoderImageKey;
import de.hetzge.eclipse.aicoder.preferences.ContextPreferences;
import de.hetzge.eclipse.aicoder.util.LambdaExceptionUtils;

public class StickyContextEntry extends ContextEntry {

	private StickyContextEntry(List<? extends ContextEntry> childContextEntries, Duration creationDuration) {
		super(childContextEntries, creationDuration);
	}

	public static final String PREFIX = "STICKY";

	@Override
	public ContextEntryKey getKey() {
		return new ContextEntryKey(PREFIX, PREFIX);
	}

	@Override
	public String getLabel() {
		return "Sticky";
	}

	@Override
	public String getContent(ContextContext context) {
		return super.getContent(context) + "\n";
	}

	@Override
	public Image getImage() {
		return AiCoderActivator.getImage(AiCoderImageKey.PIN_ICON);
	}

	public static StickyContextEntry create() throws CoreException {
		final long before = System.currentTimeMillis();
		final List<? extends ContextEntry> entries = ContextPreferences.getStickylist().stream()
				.map(LambdaExceptionUtils.rethrowFunction(Context::create))
				.flatMap(Optional::stream)
				.toList();
		return new StickyContextEntry(entries, Duration.ofMillis(System.currentTimeMillis() - before));
	}
}
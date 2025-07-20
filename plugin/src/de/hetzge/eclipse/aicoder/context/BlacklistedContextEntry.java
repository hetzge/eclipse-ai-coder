package de.hetzge.eclipse.aicoder.context;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.graphics.Image;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.AiCoderImageKey;
import de.hetzge.eclipse.aicoder.LambdaExceptionUtils;
import de.hetzge.eclipse.aicoder.preferences.ContextPreferences;

public class BlacklistedContextEntry extends ContextEntry {
	public static final String PREFIX = "BLACKLISTED";

	private BlacklistedContextEntry(List<? extends ContextEntry> childContextEntries, Duration creationDuration) {
		super(childContextEntries, creationDuration);
	}

	@Override
	public ContextEntryKey getKey() {
		return new ContextEntryKey(PREFIX, PREFIX);
	}

	@Override
	public String getLabel() {
		return "Blacklist";
	}

	@Override
	public String getContent(ContextContext context) {
		return super.getContent(context) + "\n";
	}

	@Override
	public Image getImage() {
		return AiCoderActivator.getImage(AiCoderImageKey.BLACKLIST_ICON);
	}

	public static BlacklistedContextEntry create() throws CoreException {
		final long before = System.currentTimeMillis();
		final List<? extends ContextEntry> entries = ContextPreferences.getBlacklist().stream()
				.map(LambdaExceptionUtils.rethrowFunction(Context::create))
				.flatMap(Optional::stream)
				.toList();
		return new BlacklistedContextEntry(entries, Duration.ofMillis(System.currentTimeMillis() - before));
	}
}
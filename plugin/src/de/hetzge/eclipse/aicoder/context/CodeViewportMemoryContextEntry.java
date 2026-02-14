package de.hetzge.eclipse.aicoder.context;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.runtime.CoreException;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.util.ContextUtils;

// TODO exclude fill in middle context
// TODO filter by current file ending

public final class CodeViewportMemoryContextEntry extends ContextEntry {

	public static final String LABEL = "Code Viewport Memory";
	public static final String PREFIX = "CODE_VIEWPORT_MEMORY";

	private CodeViewportMemoryContextEntry(List<? extends ContextEntry> childContextEntries, Duration creationDuration) {
		super(childContextEntries, creationDuration);
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
	public String getContent(ContextContext context) {
		final String report = AiCoderActivator.getDefault().getEditorViewMemory().getReport();
		return ContextUtils.contentTemplate("Code Viewport Memory", report != null ? report : "No report available");
	}

	@Override
	public List<? extends ContextEntry> getChildContextEntries() {
		return Collections.emptyList();
	}

	public static ContextEntryFactory factory() {
		return new ContextEntryFactory(PREFIX,
				() -> create(),
				() -> new EmptyContextEntry(PREFIX, LABEL, null));
	}

	public static CodeViewportMemoryContextEntry create() throws CoreException {
		final long before = System.currentTimeMillis();
		return new CodeViewportMemoryContextEntry(
				Collections.emptyList(),
				Duration.ofMillis(System.currentTimeMillis() - before));
	}

	public static Optional<CodeViewportMemoryContextEntry> create(ContextEntryKey key) throws CoreException {
		if (!key.prefix().equals(PREFIX)) {
			return Optional.empty();
		}
		return Optional.of(new CodeViewportMemoryContextEntry(
				Collections.emptyList(),
				Duration.ofMillis(0)));
	}
}
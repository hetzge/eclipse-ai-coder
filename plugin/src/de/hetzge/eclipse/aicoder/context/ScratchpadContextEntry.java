package de.hetzge.eclipse.aicoder.context;

import java.time.Duration;
import java.util.List;

import org.eclipse.swt.graphics.Image;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.AiCoderImageKey;
import de.hetzge.eclipse.aicoder.ScratchpadStorage;
import de.hetzge.eclipse.aicoder.util.ContextUtils;

public class ScratchpadContextEntry extends ContextEntry {

	public static final String LABEL = "Scratchpad";
	public static final String PREFIX = "SCRATCHPAD";

	private final String content;

	public ScratchpadContextEntry(String content, Duration creationDuration) {
		super(List.of(), creationDuration);
		this.content = content;
	}

	@Override
	public ContextEntryKey getKey() {
		return new ContextEntryKey(PREFIX, PREFIX);
	}

	@Override
	public String getContent(ContextContext context) {
		if (!ScratchpadStorage.isEnabled()) {
			return "";
		}
		return ContextUtils.codeTemplate(LABEL, this.content);
	}

	@Override
	public String getLabel() {
		return LABEL;
	}

	@Override
	public Image getImage() {
		return AiCoderActivator.getImage(AiCoderImageKey.SCRATCHPAD_ICON);
	}

	@Override
	public List<? extends ContextEntry> getChildContextEntries() {
		return List.of();
	}

	public static ContextEntryFactory factory() {
		return new ContextEntryFactory(PREFIX, ScratchpadContextEntry::create, () -> new EmptyContextEntry(PREFIX, LABEL, AiCoderImageKey.SCRATCHPAD_ICON));
	}

	public static ScratchpadContextEntry create() {
		final long before = System.currentTimeMillis();
		return new ScratchpadContextEntry(ScratchpadStorage.getContent(), Duration.ofMillis(System.currentTimeMillis() - before));
	}
}

package de.hetzge.eclipse.aicoder.context;

import java.time.Duration;
import java.util.List;

import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.AiCoderImageKey;
import de.hetzge.eclipse.aicoder.util.ContextUtils;

public class ClipboardContextEntry extends ContextEntry {

	public static final String PREFIX = "CLIPBOARD";

	private final String content;

	public ClipboardContextEntry(String content, Duration creationDuration) {
		super(List.of(), creationDuration);
		this.content = content;
	}

	@Override
	public String getContent(ContextContext context) {
		return ContextUtils.codeTemplate("Clipboard content", this.content);
	}

	@Override
	public String getLabel() {
		return "Clipboard";
	}

	@Override
	public ContextEntryKey getKey() {
		return new ContextEntryKey(PREFIX, PREFIX);
	}

	@Override
	public Image getImage() {
		return AiCoderActivator.getImage(AiCoderImageKey.COPY_ICON);
	}

	@Override
	public List<? extends ContextEntry> getChildContextEntries() {
		return List.of();
	}

	public static ContextEntryFactory factory() {
		return new ContextEntryFactory(PREFIX, () -> create());
	}

	public static ClipboardContextEntry create() {
		final long before = System.currentTimeMillis();
		return Display.getDefault().syncCall(() -> {
			final String clipboardContent = (String) new Clipboard(Display.getDefault()).getContents(TextTransfer.getInstance());
			return new ClipboardContextEntry(clipboardContent != null ? clipboardContent : "", Duration.ofMillis(System.currentTimeMillis() - before));
		});
	}
}
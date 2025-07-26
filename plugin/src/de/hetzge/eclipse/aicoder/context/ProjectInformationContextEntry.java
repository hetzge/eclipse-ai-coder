package de.hetzge.eclipse.aicoder.context;

import java.time.Duration;
import java.util.List;

import org.eclipse.swt.graphics.Image;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.AiCoderImageKey;
import de.hetzge.eclipse.aicoder.util.ContextUtils;

public class ProjectInformationContextEntry extends ContextEntry {

	public static final String PREFIX = "PROJECT_INFORMATION";

	private final String key;
	private final String value;

	private ProjectInformationContextEntry(Duration creationDuration, String key, String value) {
		super(List.of(), creationDuration);
		this.key = key;
		this.value = value;
	}

	@Override
	public ContextEntryKey getKey() {
		return new ContextEntryKey(PREFIX, this.key);
	}

	@Override
	public String getLabel() {
		return String.format("%s: %s", this.key, this.value);
	}

	@Override
	public Image getImage() {
		return AiCoderActivator.getImage(AiCoderImageKey.INFORMATIONS_ICON);
	}

	@Override
	public String getContent(ContextContext context) {
		return ContextUtils.listEntryTemplate(String.format("%s: %s", this.key, this.value));
	}

	public static ProjectInformationContextEntry create(String key, String value) {
		return new ProjectInformationContextEntry(Duration.ZERO, key, value);
	}
}

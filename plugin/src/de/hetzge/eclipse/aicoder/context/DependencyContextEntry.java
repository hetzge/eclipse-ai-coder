package de.hetzge.eclipse.aicoder.context;

import java.time.Duration;
import java.util.List;

import org.eclipse.swt.graphics.Image;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.AiCoderImageKey;
import de.hetzge.eclipse.aicoder.util.ContextUtils;

public class DependencyContextEntry extends ContextEntry {

	private static final String DEPENDENCY = "dependency";

	private final String dependency;

	private DependencyContextEntry(Duration creationDuration, String dependency) {
		super(List.of(), creationDuration);
		this.dependency = dependency;
	}

	@Override
	public ContextEntryKey getKey() {
		return new ContextEntryKey(DEPENDENCY, this.dependency);
	}

	@Override
	public String getLabel() {
		return this.dependency;
	}

	@Override
	public String getContent(ContextContext context) {
		return ContextUtils.listEntryTemplate(this.dependency);
	}

	@Override
	public Image getImage() {
		return AiCoderActivator.getImage(AiCoderImageKey.DEPENDENCIES_ICON);
	}

	public static DependencyContextEntry create(String dependency) {
		return new DependencyContextEntry(Duration.ZERO, dependency);
	}
}

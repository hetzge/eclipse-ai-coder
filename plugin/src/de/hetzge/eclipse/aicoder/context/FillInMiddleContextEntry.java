package de.hetzge.eclipse.aicoder.context;

import java.time.Duration;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.graphics.Image;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.AiCoderImageKey;
import de.hetzge.eclipse.aicoder.config.ContextConfig.FillInMiddleConfig;
import de.hetzge.eclipse.aicoder.config.TaskConfig;
import de.hetzge.eclipse.aicoder.preferences.AiCoderPreferences;
import de.hetzge.eclipse.aicoder.util.ContextUtils;

public class FillInMiddleContextEntry extends ContextEntry {

	public static final String LABEL = "Fill in the middle";
	public static final String FILL_HERE_PLACEHOLDER = "<<<<%s_%s>>>>".formatted("FILL", "HERE"); // formatted to avoid confusing ai coder in this file ;)
	public static final String PREFIX = "FILL_IN_MIDDLE";

	private final String filename;
	private final String prefix;
	private final String suffix;

	private FillInMiddleContextEntry(String filename, String prefix, String suffix, Duration creationDuration) {
		super(List.of(), creationDuration);
		this.filename = filename;
		this.prefix = prefix;
		this.suffix = suffix;
	}

	@Override
	public ContextEntryKey getKey() {
		return new ContextEntryKey(PREFIX, PREFIX);
	}

	@Override
	public String getLabel() {
		return LABEL;
	}

	@Override
	public Image getImage() {
		return AiCoderActivator.getImage(AiCoderImageKey.FILL_IN_MIDDLE_ICON);
	}

	@Override
	public String getContent(ContextContext context) {
		return ContextUtils.contentTemplate(String.format("Current edit location: %s", this.filename), this.prefix + FILL_HERE_PLACEHOLDER + this.suffix);
	}

	public static ContextEntryFactory factory(String filename, IDocument document, int modelOffset, TaskConfig config) {
		return new ContextEntryFactory(PREFIX, () -> create(filename, document, modelOffset, config), () -> new EmptyContextEntry(PREFIX, LABEL, AiCoderImageKey.FILL_IN_MIDDLE_ICON));
	}

	public static FillInMiddleContextEntry create(String filename, IDocument document, int modelOffset, TaskConfig config) throws CoreException {
		final long before = System.currentTimeMillis();
		final String prefix = getPrefix(document, modelOffset, config);
		final String suffix = getSuffix(document, modelOffset, config);
		return new FillInMiddleContextEntry(filename, prefix, suffix, Duration.ofMillis(System.currentTimeMillis() - before));
	}

	public static String getPrefix(IDocument document, int modelOffset, TaskConfig config) throws CoreException {
		try {
			final int firstLine = calculateFirstLine(document, modelOffset, config);
			final int lineOffset = document.getLineOffset(firstLine);
			final int length = modelOffset - document.getLineOffset(firstLine);
			return document.get(lineOffset, length);
		} catch (final BadLocationException exception) {
			throw new CoreException(Status.error("Failed to get prefix", exception));
		}
	}

	public static String getSuffix(IDocument document, int modelOffset, TaskConfig config) throws CoreException {
		try {
			final int lastLine = calculateLastLine(document, modelOffset, config);
			final int lineOffset = modelOffset;
			final int length = document.getLineOffset(lastLine) + document.getLineLength(lastLine) - modelOffset;
			return document.get(lineOffset, length);
		} catch (final BadLocationException exception) {
			throw new CoreException(Status.error("Failed to get suffix", exception));
		}
	}

	public static int calculateFirstLine(IDocument document, int modelOffset, TaskConfig config) throws BadLocationException {
		final int modelLine = document.getLineOfOffset(modelOffset);
		final int maxLines = config.getContextConfig(FillInMiddleContextEntry.PREFIX)
				.map(FillInMiddleConfig.class::cast)
				.map(FillInMiddleConfig::getMaxPrefixLength)
				.orElseGet(() -> AiCoderPreferences.getMaxPrefixSize());
		return Math.max(0, modelLine - maxLines);
	}

	public static int calculateLastLine(IDocument document, int modelOffset, TaskConfig config) throws BadLocationException {
		final int modelLine = document.getLineOfOffset(modelOffset);
		final int maxLines = config.getContextConfig(FillInMiddleContextEntry.PREFIX)
				.map(FillInMiddleConfig.class::cast)
				.map(FillInMiddleConfig::getMaxSuffixLength)
				.orElseGet(() -> AiCoderPreferences.getMaxSuffixSize());
		return Math.min(document.getNumberOfLines() - 1, modelLine + maxLines + 1);
	}
}

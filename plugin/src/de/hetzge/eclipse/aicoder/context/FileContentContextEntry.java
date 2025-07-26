package de.hetzge.eclipse.aicoder.context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

import de.hetzge.eclipse.aicoder.util.ContextUtils;

public final class FileContentContextEntry extends ContextEntry {

	public static final String PREFIX = "FILE_CONTENT";

	private final String fileName;
	private final String content;

	private FileContentContextEntry(String fileName, String content, Duration creationDuration) {
		super(List.of(), creationDuration);
		this.fileName = fileName;
		this.content = content;
	}

	@Override
	public String getLabel() {
		return this.fileName;
	}

	@Override
	public String getContent(ContextContext context) {
		return ContextUtils.codeTemplate("File: " + this.fileName, this.content);
	}

	@Override
	public ContextEntryKey getKey() {
		return new ContextEntryKey(PREFIX, this.fileName);
	}

	public static FileContentContextEntry create(IFile file) throws CoreException {
		final long before = System.currentTimeMillis();
		final String fileName = file.getName();
		final String content = readFileContent(file);
		return new FileContentContextEntry(fileName, content, Duration.ofMillis(System.currentTimeMillis() - before));
	}

	public static FileContentContextEntry create(String fileName, String content) throws CoreException {
		return new FileContentContextEntry(fileName, content, Duration.ZERO);
	}

	private static String readFileContent(IFile file) throws CoreException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getContents(), StandardCharsets.UTF_8))) {
			return reader.lines().collect(Collectors.joining("\n"));
		} catch (final IOException exception) {
			throw new CoreException(org.eclipse.core.runtime.Status.error("Failed to read file content", exception));
		}
	}
}
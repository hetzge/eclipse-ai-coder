package de.hetzge.eclipse.aicoder.context;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.AiCoderImageKey;
import de.hetzge.eclipse.aicoder.history.AiCoderHistoryEntry;
import de.hetzge.eclipse.aicoder.history.AiCoderHistoryView;
import de.hetzge.eclipse.aicoder.history.HistoryType;
import de.hetzge.eclipse.aicoder.llm.LlmPromptTemplates;
import de.hetzge.eclipse.aicoder.llm.LlmResponse;
import de.hetzge.eclipse.aicoder.llm.LlmUtils;
import de.hetzge.eclipse.aicoder.util.ContextUtils;
import de.hetzge.eclipse.aicoder.util.EclipseUtils;
import de.hetzge.eclipse.aicoder.util.FileTreeUtils;
import de.hetzge.eclipse.aicoder.util.LambdaExceptionUtils;

public class AiRerankContextEntry extends ContextEntry {

	public static final String LABEL = "AI Rerank";
	public static final String PREFIX = "AI_RERANK";

	public AiRerankContextEntry(List<ContextEntry> childContextEntries, Duration creationDuration) {
		super(childContextEntries, creationDuration);
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
		return AiCoderActivator.getImage(AiCoderImageKey.RERANK_ICON);
	}

	@Override
	public String getContent(ContextContext context) {
		return ContextUtils.contentTemplate("Relevant files", super.getContent(context));
	}

	public static ContextEntryFactory factory(IDocument document, IEditorInput editorInput, String originalInstructions, int modelOffset) {
		return new ContextEntryFactory(PREFIX, () -> create(document, editorInput, originalInstructions, modelOffset), () -> new EmptyContextEntry(PREFIX, LABEL, AiCoderImageKey.RERANK_ICON));
	}

	public static AiRerankContextEntry create(IDocument document, IEditorInput editorInput, String originalInstructions, int modelOffset) throws CoreException {
		final long before = System.currentTimeMillis();
		if (editorInput instanceof final IFileEditorInput fileEditorInput) {
			final IFile file = fileEditorInput.getFile();
			final IProject project = file.getProject();
			final int maxRerankResultPaths = 20;
			final String systemPrompt = LlmPromptTemplates.rerankSystemPrompt(maxRerankResultPaths);
			final String prefix = FillInMiddleContextEntry.getPrefix(document, modelOffset);
			final String suffix = FillInMiddleContextEntry.getSuffix(document, modelOffset);
			final String currentFileName = EclipseUtils.getFilename(editorInput).orElse("Unknown file");
			final String instructions = LlmPromptTemplates.rerankPrompt(FileTreeUtils.createResourceTreeString(project), originalInstructions, prefix, suffix, currentFileName);
			try {
				final LlmResponse llmResponse = LlmUtils.executeRerank(systemPrompt, instructions).get(1, TimeUnit.MINUTES);
				AiCoderHistoryView.get().ifPresent(view -> {
					Display.getDefault().asyncExec(() -> {
						final AiCoderHistoryEntry historyEntry = new AiCoderHistoryEntry(HistoryType.RERANK, file.getName(), null);
						historyEntry.setInput(instructions);
						historyEntry.setupLlmResponse(llmResponse);
						view.addHistoryEntry(historyEntry);
					});
				});
				if (llmResponse.isError()) {
					final Duration creationDuration = Duration.ofMillis(System.currentTimeMillis() - before);
					return new AiRerankContextEntry(List.of(), creationDuration);
				}
				final List<Path> paths = parseRerankOutput(llmResponse.getContent(), maxRerankResultPaths);
				final List<IFile> files = paths.stream()
						.map(path -> project.getFile(path.toString()))
						.filter(IFile::exists)
						.toList();
				final List<ContextEntry> childContextEntries = files.parallelStream()
						.filter(it -> !it.equals(file)) // exclude current file
						.flatMap(LambdaExceptionUtils.rethrowFunction(it -> {
							final IJavaElement element = JavaCore.create(it);
							if (element instanceof final ICompilationUnit compilationUnit) {
								return OpenEditorsContextEntry.createTypeContextEntry(compilationUnit);
							} else {
								return Stream.of(FileContentContextEntry.create(it));
							}
						}))
						.toList();
				final Duration creationDuration = Duration.ofMillis(System.currentTimeMillis() - before);
				return new AiRerankContextEntry(childContextEntries, creationDuration);
			} catch (InterruptedException | ExecutionException | TimeoutException exception) {
				AiCoderActivator.log().error("Failed to create AI rerank context entry.", exception);
				final Duration creationDuration = Duration.ofMillis(System.currentTimeMillis() - before);
				return new AiRerankContextEntry(List.of(), creationDuration);
			}
		} else {
			return new AiRerankContextEntry(List.of(), Duration.ZERO);
		}
	}

	private static List<Path> parseRerankOutput(String output, int maxRerankResultPaths) {
		final List<Path> locations = new ArrayList<>();
		final String[] lines = output.split("\n");
		for (String line : lines) {
			line = line.trim();
			if (line.startsWith("-")) {
				final String pathString = line.substring(1).trim();
				locations.add(Path.of(pathString.startsWith("/") ? pathString.substring(1) : pathString));
				if (locations.size() >= maxRerankResultPaths) {
					break;
				}
			}
		}
		return locations;
	}
}

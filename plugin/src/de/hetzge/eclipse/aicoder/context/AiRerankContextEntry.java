package de.hetzge.eclipse.aicoder.context;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
import de.hetzge.eclipse.aicoder.CompletionMode;
import de.hetzge.eclipse.aicoder.config.ContextConfig.AiRerankConfig;
import de.hetzge.eclipse.aicoder.config.TaskConfig;
import de.hetzge.eclipse.aicoder.history.AiCoderHistoryView;
import de.hetzge.eclipse.aicoder.history.HistoryEntry;
import de.hetzge.eclipse.aicoder.history.HistoryStatus;
import de.hetzge.eclipse.aicoder.llm.LlmPromptTemplates;
import de.hetzge.eclipse.aicoder.llm.LlmResponse;
import de.hetzge.eclipse.aicoder.llm.LlmUtils;
import de.hetzge.eclipse.aicoder.preferences.AiCoderPreferences;
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

	public static ContextEntryFactory factory(IDocument document, IEditorInput editorInput, String originalInstructions, int modelOffset, TaskConfig config) {
		return new ContextEntryFactory(PREFIX, () -> create(document, editorInput, originalInstructions, modelOffset, config), () -> new EmptyContextEntry(PREFIX, LABEL, AiCoderImageKey.RERANK_ICON));
	}

	public static AiRerankContextEntry create(IDocument document, IEditorInput editorInput, String originalInstructions, int modelOffset, TaskConfig config) throws CoreException {
		final long before = System.currentTimeMillis();
		if (editorInput instanceof final IFileEditorInput fileEditorInput) {
			final IFile file = fileEditorInput.getFile();
			final IProject project = file.getProject();
			final int maxRerankResultCount = config.getContextConfig(PREFIX).map(AiRerankConfig.class::cast).map(AiRerankConfig::getLimit).orElse(20);
			final String systemPrompt = LlmPromptTemplates.rerankSystemPrompt(maxRerankResultCount);
			final String prefix = FillInMiddleContextEntry.getPrefix(document, modelOffset, config);
			final String suffix = FillInMiddleContextEntry.getSuffix(document, modelOffset, config);
			final String currentFileName = EclipseUtils.getFilename(editorInput).orElse("Unknown file");
			final List<String> whitelist = config.getContextConfig(AiRerankContextEntry.PREFIX).map(AiRerankConfig.class::cast).map(AiRerankConfig::getWhitelist).orElseGet(() -> AiCoderPreferences.getAiRerankWhitelist());
			final List<String> blacklist = config.getContextConfig(AiRerankContextEntry.PREFIX).map(AiRerankConfig.class::cast).map(AiRerankConfig::getBlacklist).orElseGet(() -> AiCoderPreferences.getAiRerankBlacklist());
			final String instructions = LlmPromptTemplates.rerankPrompt(FileTreeUtils.createResourceTreeString(project, whitelist, blacklist), originalInstructions, prefix, suffix, currentFileName);
			try {
				final LlmResponse llmResponse = LlmUtils.executeRerank(systemPrompt, instructions).get(1, TimeUnit.MINUTES);
				AiCoderHistoryView.get().ifPresent(view -> {
					Display.getDefault().asyncExec(() -> {
						final HistoryEntry historyEntry = new HistoryEntry(UUID.randomUUID(), CompletionMode.DUMMY, file.getFullPath().makeRelative().toPath(), instructions, "", Optional.of(llmResponse), Duration.ofMillis(System.currentTimeMillis() - before), HistoryStatus.ACCEPTED);
						view.addHistoryEntry(historyEntry);
					});
				});
				if (!llmResponse.isSuccess()) {
					final Duration creationDuration = Duration.ofMillis(System.currentTimeMillis() - before);
					return new AiRerankContextEntry(List.of(), creationDuration);
				}
				final List<Path> paths = parseRerankOutput(project, llmResponse.getContent(), maxRerankResultCount);
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

	private static List<Path> parseRerankOutput(IProject project, String output, int maxRerankResultCount) {
		final String projectName = project.getName();
		final String absoluteProjectPath = project.getLocation().toString();
		final List<Path> locations = new ArrayList<>();
		for (String line : output.lines().toList()) {
			line = line.trim();
			if (line.startsWith("-")) {
				String pathString = line.substring(1).trim();
				// remove absolute project path
				pathString = pathString.startsWith(absoluteProjectPath) ? pathString.replace(absoluteProjectPath + "/", "") : pathString;
				// remove leading slash
				pathString = pathString.startsWith("/") ? pathString.substring(1) : pathString;
				// remove project name from path
				pathString = pathString.startsWith(projectName) ? pathString.replace(projectName + "/", "") : pathString;
				locations.add(Path.of(pathString));
				if (locations.size() >= maxRerankResultCount) {
					break;
				}
			}
		}
		return locations;
	}

}

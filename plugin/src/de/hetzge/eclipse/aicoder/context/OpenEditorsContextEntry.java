package de.hetzge.eclipse.aicoder.context;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.AiCoderImageKey;
import de.hetzge.eclipse.aicoder.util.ContextUtils;
import de.hetzge.eclipse.aicoder.util.EclipseUtils;
import de.hetzge.eclipse.aicoder.util.LambdaExceptionUtils;
import de.hetzge.eclipse.aicoder.util.Utils;

public class OpenEditorsContextEntry extends ContextEntry {

	public static final String PREFIX = "OPEN_EDITORS";

	private OpenEditorsContextEntry(List<? extends ContextEntry> childContextEntries, Duration creationDuration) {
		super(childContextEntries, creationDuration);
	}

	@Override
	public ContextEntryKey getKey() {
		return new ContextEntryKey(PREFIX, PREFIX);
	}

	@Override
	public String getLabel() {
		return "Open editors";
	}

	@Override
	public Image getImage() {
		return AiCoderActivator.getImage(AiCoderImageKey.EDITOR_ICON);
	}

	@Override
	public String getContent(ContextContext context) {
		return ContextUtils.contentTemplate("Open editors", super.getContent(context));
	}

	public static ContextEntryFactory factory() {
		return new ContextEntryFactory(PREFIX, () -> create());
	}

	public static OpenEditorsContextEntry create() throws CoreException {
		final long before = System.currentTimeMillis();
		final List<? extends ContextEntry> entries = createEntries();
		return new OpenEditorsContextEntry(entries, Duration.ofMillis(System.currentTimeMillis() - before));
	}

	private static List<? extends ContextEntry> createEntries() throws CoreException {
		return Optional.ofNullable(Display.getDefault().syncCall(PlatformUI.getWorkbench()::getActiveWorkbenchWindow))
				.map(IWorkbenchWindow::getActivePage)
				.map(IWorkbenchPage::getEditorReferences)
				.map(Arrays::asList)
				.orElse(List.of())
				.stream()
				// exclude current active editor
				.filter(editorRef -> !EclipseUtils.isActiveEditor(editorRef.getEditor(false)))
				.flatMap(LambdaExceptionUtils.rethrowFunction(editorRef -> {
					try {
						final IEditorInput input = editorRef.getEditorInput();
						final Optional<ICompilationUnit> unitOptional = EclipseUtils.getCompilationUnit(input);
						if (unitOptional.isPresent()) {
							return createTypeContextEntry(unitOptional.get());
						} else {
							return createFileContextEntry(input, editorRef.getPart(false));
						}
					} catch (final PartInitException exception) {
						AiCoderActivator.log().info(String.format("Skipping editor '%s' due to error: %s", editorRef.getTitle(), exception.getMessage()));
						return Stream.empty();
					}
				}))
				.toList();
	}

	private static Stream<? extends ContextEntry> createTypeContextEntry(ICompilationUnit unit) throws CoreException {
		if (!unit.exists()) {
			return Stream.empty();
		}
		return Optional.ofNullable(unit)
				.map(LambdaExceptionUtils.rethrowFunction(ICompilationUnit::getAllTypes))
				.stream()
				.flatMap(Arrays::stream)
				.filter(Utils::checkType)
				.map(LambdaExceptionUtils.rethrowFunction(TypeContextEntry::create));
	}

	private static Stream<? extends ContextEntry> createFileContextEntry(IEditorInput input, IWorkbenchPart part) throws CoreException {
		if (part == null) {
			return Stream.empty();
		}
		try {
			return EclipseUtils.stringFromInput(input, part).map(LambdaExceptionUtils.rethrowFunction(content -> FileContentContextEntry.create(part.getTitle(), content))).stream();
		} catch (final IOException exception) {
			throw new CoreException(Status.error("Failed to read file content", exception));
		}
	}
}
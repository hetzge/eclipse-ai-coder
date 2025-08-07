package de.hetzge.eclipse.aicoder.context;

import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorInput;

import de.hetzge.eclipse.aicoder.preferences.ContextPreferences;
import de.hetzge.eclipse.aicoder.util.EclipseUtils;
import de.hetzge.eclipse.aicoder.util.LambdaExceptionUtils;

public class RootContextEntry extends ContextEntry {
	public static final String PREFIX = "ROOT";

	public RootContextEntry(List<? extends ContextEntry> childContextEntries, Duration creationDuration) {
		super(childContextEntries, creationDuration);
	}

	@Override
	public String getLabel() {
		return "Root";
	}

	@Override
	public String getContent(ContextContext context) {
		return super.getContent(context) + "\n";
	}

	@Override
	public ContextEntryKey getKey() {
		return new ContextEntryKey(PREFIX, "ROOT");
	}

	public static RootContextEntry create(IDocument document, IEditorInput editorInput, int offset) throws BadLocationException, UnsupportedFlavorException, IOException, CoreException {
		final IProject project = EclipseUtils.getProject(editorInput);
		final String filename = EclipseUtils.getFilename(editorInput).orElse("Active File");
		final long before = System.currentTimeMillis();
		final Optional<ICompilationUnit> compilationUnitOptional = EclipseUtils.getCompilationUnit(editorInput);
		final List<ContextEntryFactory> factories = new ArrayList<>();
		factories.add(ProjectInformationsContextEntry.factory(project));
		factories.add(FileTreeContextEntry.factory(editorInput));
		factories.add(DependenciesContextEntry.factory(project));
		factories.add(OpenEditorsContextEntry.factory());
		factories.add(StickyContextEntry.factory());
		factories.add(UserContextEntry.factory());
		if (compilationUnitOptional.isPresent()) {
			final ICompilationUnit unit = compilationUnitOptional.get();
			factories.add(SuperContextEntry.factory(unit, offset));
			factories.add(ScopeContextEntry.factory(unit, offset));
			factories.add(ImportsContextEntry.factory(unit));
			factories.add(PackageContextEntry.factory(unit));
		}
		factories.add(LastEditsContextEntry.factory());
		factories.add(ClipboardContextEntry.factory());
		factories.add(FillInMiddleContextEntry.factory(filename, document, offset));
		final List<String> orderedPrefixes = ContextPreferences.getContextTypePositions().stream()
				.filter(item -> item.enabled())
				.map(item -> item.prefix())
				.toList();
		final List<ContextEntry> filteredAndSortedEntries = factories.parallelStream()
				.filter(factory -> orderedPrefixes.contains(factory.prefix()))
				.map(LambdaExceptionUtils.rethrowFunction(factory -> factory.supplier().get()))
				.sorted(Comparator.comparingInt(entry -> orderedPrefixes.indexOf(entry.getKey().prefix())))
				.toList();
		return new RootContextEntry(filteredAndSortedEntries, Duration.ofMillis(System.currentTimeMillis() - before));
	}
}
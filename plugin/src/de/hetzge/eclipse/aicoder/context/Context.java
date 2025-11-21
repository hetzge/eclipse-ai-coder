package de.hetzge.eclipse.aicoder.context;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.eclipse.core.runtime.CoreException;

import de.hetzge.eclipse.aicoder.util.LambdaExceptionUtils;

/*
 * TODO lazy create methods
 * TODO show tokens of each entry
 * TODO last edited files
 * TODO last viewed files
 * TODO manually ranked files
 * TODO ignore items (ignore-file and storage)
 * TODO add java/maven metadata (Java Version, Dependencies)
 * TODO https://github.com/continuedev/continue/blob/main/core/autocomplete/postprocessing/index.ts
 * TODO ContentContextEntry (full file content, example/reference content)
 * TODO Resource context menu to add sticky type/content
 * TODO if in overriden method add super javadoc
 * TODO super class context entry
 * TODO predictiv context ("new Typename(<complete>" ... lookup class "Typename" even if not imported)
 * TODO open tabs context (same type)
 * TODO configure last x entries (tabs, edited files, usw.)
 * TODO files in current folder context
 * TODO trigger inline completion when rename is entered
 * TODO trigger comment complete (ctrl + shift + space when comment is selected)
 * TODO trigger ai refactor (ctrl + shift + space when code is selected)
 * TODO mark context entries with error that occured while creation
 * TODO mark sticky / disabled in resource tree
 * TODO multiple suggestions (count configurable, or as long as user does not select one)
 * TODO grep blacklist for files and folders (and use gitignore) -> use for File Tree context
 */

public final class Context {

	// TODO use enum for prefixes, label, order

	public static final Map<String, String> CONTEXT_TYPE_NAME_BY_CONTEXT_PREFIX = Map.ofEntries(
			Map.entry(ProjectInformationsContextEntry.PREFIX, "Project informations"),
			Map.entry(DependenciesContextEntry.PREFIX, "Dependencies"),
			Map.entry(OpenEditorsContextEntry.PREFIX, "Open editors"),
			Map.entry(ImportsContextEntry.PREFIX, "Imports (Java)"),
			Map.entry(SuperContextEntry.PREFIX, "Super (Java)"),
			Map.entry(FileTreeContextEntry.PREFIX, "File tree"),
			Map.entry(StickyContextEntry.PREFIX, "Sticky"),
			Map.entry(TypeContextEntry.PREFIX, "Type (Java)"),
			Map.entry(FillInMiddleContextEntry.PREFIX, "Fill in middle"),
			Map.entry(CustomContextEntry.PREFIX, "Custom"),
			Map.entry(ClipboardContextEntry.PREFIX, "Clipboard"),
			Map.entry(LastEditsContextEntry.PREFIX, "Last edits"),
			Map.entry(EmptyContextEntry.PREFIX, "Empty"),
			Map.entry(BlacklistedContextEntry.PREFIX, "Blacklisted"),
			Map.entry(ScopeContextEntry.PREFIX, "Scope (Java)"),
			Map.entry(UserContextEntry.PREFIX, "User"),
			Map.entry(RootContextEntry.PREFIX, "Root"),
			Map.entry(TypeMemberContextEntry.PREFIX, "Type member (Java)"),
			Map.entry(PackageContextEntry.PREFIX, "Package (Java)"));

	public static final List<String> DEFAULT_PREFIX_ORDER = List.of(
			ProjectInformationsContextEntry.PREFIX,
			FileTreeContextEntry.PREFIX,
			DependenciesContextEntry.PREFIX,
			OpenEditorsContextEntry.PREFIX,
			SuperContextEntry.PREFIX,
			ScopeContextEntry.PREFIX,
			ImportsContextEntry.PREFIX,
			PackageContextEntry.PREFIX,
			StickyContextEntry.PREFIX,
			UserContextEntry.PREFIX,
			LastEditsContextEntry.PREFIX,
			ClipboardContextEntry.PREFIX,
			FillInMiddleContextEntry.PREFIX);

	public static final Set<String> DEFAULT_ACTIVE_PREFIXES = Set.of(FillInMiddleContextEntry.PREFIX);

	public static Optional<? extends ContextEntry> create(ContextEntryKey key) throws CoreException {
		final List<? extends Function<ContextEntryKey, Optional<? extends ContextEntry>>> factories = List.of(
				LambdaExceptionUtils.rethrowFunction(TypeMemberContextEntry::create),
				LambdaExceptionUtils.rethrowFunction(PackageContextEntry::create),
				LambdaExceptionUtils.rethrowFunction(TypeContextEntry::create),
				LambdaExceptionUtils.rethrowFunction(CustomContextEntryData::create));
		return factories.stream().flatMap(factory -> factory.apply(key).stream()).findFirst();
	}
}
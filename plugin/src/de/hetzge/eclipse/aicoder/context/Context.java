package de.hetzge.eclipse.aicoder.context;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.eclipse.core.runtime.CoreException;

import de.hetzge.eclipse.aicoder.LambdaExceptionUtils;

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
 */

public final class Context {

	public static final Map<String, String> CONTEXT_TYPE_NAME_BY_CONTEXT_PREFIX = Map.ofEntries(
			Map.entry(ImportsContextEntry.PREFIX, "Imports"),
			Map.entry(StickyContextEntry.PREFIX, "Sticky"),
			Map.entry(TypeContextEntry.PREFIX, "Type"),
			Map.entry(PrefixContextEntry.PREFIX, "Prefix"),
			Map.entry(CustomContextEntry.PREFIX, "Custom"),
			Map.entry(ClipboardContextEntry.PREFIX, "Clipboard"),
			Map.entry(EmptyContextEntry.PREFIX, "Empty"),
			Map.entry(BlacklistedContextEntry.PREFIX, "Blacklisted"),
			Map.entry(SuffixContextEntry.PREFIX, "Suffix"),
			Map.entry(ScopeContextEntry.PREFIX, "Scope"),
			Map.entry(UserContextEntry.PREFIX, "User"),
			Map.entry(RootContextEntry.PREFIX, "Root"),
			Map.entry(TypeMemberContextEntry.PREFIX, "Type Member"),
			Map.entry(PackageContextEntry.PREFIX, "Package"));

	public static final List<String> DEFAULT_PREFIX_ORDER = List.of(
			RootContextEntry.PREFIX,
			ScopeContextEntry.PREFIX,
			ImportsContextEntry.PREFIX,
			PackageContextEntry.PREFIX,
			TypeContextEntry.PREFIX,
			TypeMemberContextEntry.PREFIX,
			PrefixContextEntry.PREFIX,
			SuffixContextEntry.PREFIX,
			ClipboardContextEntry.PREFIX,
			StickyContextEntry.PREFIX,
			CustomContextEntry.PREFIX,
			UserContextEntry.PREFIX,
			BlacklistedContextEntry.PREFIX,
			EmptyContextEntry.PREFIX);

	public static Optional<? extends ContextEntry> create(ContextEntryKey key) throws CoreException {
		final List<? extends Function<ContextEntryKey, Optional<? extends ContextEntry>>> factories = List.of(
				LambdaExceptionUtils.rethrowFunction(TypeMemberContextEntry::create),
				LambdaExceptionUtils.rethrowFunction(PackageContextEntry::create),
				LambdaExceptionUtils.rethrowFunction(TypeContextEntry::create),
				LambdaExceptionUtils.rethrowFunction(CustomContextEntry::create));
		return factories.stream().flatMap(factory -> factory.apply(key).stream()).findFirst();
	}
}
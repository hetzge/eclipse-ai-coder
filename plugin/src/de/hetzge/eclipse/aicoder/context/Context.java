package de.hetzge.eclipse.aicoder.context;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.eclipse.core.runtime.CoreException;

import de.hetzge.eclipse.aicoder.LambdaExceptionUtils;

/*
 * TODO lazy create methods
 * TODO Prio levels (High, Medium, Low)
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
 */

public final class Context {

	public static Optional<? extends ContextEntry> create(ContextEntryKey key) throws CoreException {
		final List<? extends Function<ContextEntryKey, Optional<? extends ContextEntry>>> factories = List.of(
				LambdaExceptionUtils.rethrowFunction(TypeMemberContextEntry::create),
				LambdaExceptionUtils.rethrowFunction(PackageContextEntry::create),
				LambdaExceptionUtils.rethrowFunction(TypeContextEntry::create),
				LambdaExceptionUtils.rethrowFunction(CustomContextEntry::create));
		return factories.stream().flatMap(factory -> factory.apply(key).stream()).findFirst();
	}
}
package de.hetzge.eclipse.aicoder.context;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import de.hetzge.eclipse.aicoder.util.ContextUtils;
import de.hetzge.eclipse.aicoder.util.LambdaExceptionUtils;

public class SuperContextEntry extends ContextEntry {
	public static final String PREFIX = "SUPER";

	private SuperContextEntry(List<? extends ContextEntry> childContextEntries, Duration creationDuration) {
		super(childContextEntries, creationDuration);
	}

	@Override
	public ContextEntryKey getKey() {
		return new ContextEntryKey(PREFIX, PREFIX);
	}

	@Override
	public String getLabel() {
		return "Super types";
	}

	@Override
	public String getContent(ContextContext context) {
		return ContextUtils.contentTemplate("Supertypes (classes and interfaces)", super.getContent(context));
	}

	public static ContextEntryFactory factory(ICompilationUnit unit, int offset) {
		return new ContextEntryFactory(PREFIX, () -> create(unit, offset));
	}

	public static SuperContextEntry create(ICompilationUnit unit, int offset) throws JavaModelException, CoreException {
		final long before = System.currentTimeMillis();
		final List<TypeContextEntry> entries = Stream.of(unit.getTypes())
				.filter(LambdaExceptionUtils.rethrowPredicate(type -> isOffsetInsideType(offset, type)))
				.flatMap(LambdaExceptionUtils.rethrowFunction(type -> getSuperTypes(type)))
				.map(LambdaExceptionUtils.rethrowFunction(TypeContextEntry::create))
				.toList();
		return new SuperContextEntry(entries, Duration.ofMillis(System.currentTimeMillis() - before));
	}

	private static Stream<IType> getSuperTypes(IType type) throws JavaModelException {
		if (type.getFullyQualifiedName().equals(Object.class.getName())) {
			return Stream.of();
		}
		return Stream.concat(
				Optional.ofNullable(type.getSuperclassName()).stream()
						.map(LambdaExceptionUtils.rethrowFunction(name -> type.resolveType(name)))
						.filter(Objects::nonNull)
						.map(LambdaExceptionUtils.rethrowFunction(resolvedType -> type.getJavaProject().findType(resolvedType[0][0], resolvedType[0][1]))),
				Stream.of(type.getSuperInterfaceNames())
						.map(LambdaExceptionUtils.rethrowFunction(name -> type.resolveType(name)))
						.filter(Objects::nonNull)
						.map(LambdaExceptionUtils.rethrowFunction(resolvedType -> type.getJavaProject().findType(resolvedType[0][0], resolvedType[0][1]))))
				.flatMap(LambdaExceptionUtils.rethrowFunction(it -> Stream.concat(Stream.of(it), getSuperTypes(it))));
	}

	private static boolean isOffsetInsideType(int offset, IType type) throws JavaModelException {
		return type.getSourceRange().getOffset() <= offset && offset <= type.getSourceRange().getOffset() + type.getSourceRange().getLength();
	}

}

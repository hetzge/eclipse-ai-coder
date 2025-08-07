package de.hetzge.eclipse.aicoder.context;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.internal.corext.dom.ScopeAnalyzer;
import org.eclipse.swt.graphics.Image;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.AiCoderImageKey;
import de.hetzge.eclipse.aicoder.preferences.AiCoderPreferences;
import de.hetzge.eclipse.aicoder.util.ContextUtils;
import de.hetzge.eclipse.aicoder.util.JdkUtils;
import de.hetzge.eclipse.aicoder.util.LambdaExceptionUtils;
import de.hetzge.eclipse.aicoder.util.Utils;

public class ScopeContextEntry extends ContextEntry {

	public static final String PREFIX = "SCOPE";

	public ScopeContextEntry(List<? extends ContextEntry> childContextEntries, Duration creationDuration) {
		super(childContextEntries, creationDuration);
	}

	@Override
	public String getLabel() {
		return "Scope";
	}

	@Override
	public String getContent(ContextContext context) {
		return ContextUtils.contentTemplate("Content in scope", super.getContent(context));
	}

	@Override
	public ContextEntryKey getKey() {
		return new ContextEntryKey(PREFIX, PREFIX);
	}

	@Override
	public Image getImage() {
		return AiCoderActivator.getImage(AiCoderImageKey.SCOPE_ICON);
	}

	public static ContextEntryFactory factory(ICompilationUnit unit, int offset) {
		return new ContextEntryFactory(PREFIX, () -> create(unit, offset));
	}

	public static ScopeContextEntry create(ICompilationUnit unit, int offset) throws CoreException {
		final long before = System.currentTimeMillis();
		final CompilationUnit parsedUnit = parseUnit(unit);

		final List<TypeContextEntry> entries = getBindingsInScope(parsedUnit, offset).stream().parallel()
				.flatMap(LambdaExceptionUtils.rethrowFunction(binding -> {
					if (binding.getJavaElement() instanceof final IType type) {
						final String fullyQualifiedName = type.getFullyQualifiedName();
						if (AiCoderPreferences.isIgnoreJreClasses() && JdkUtils.isJREPackage(fullyQualifiedName)) {
							return Stream.empty();
						}
						if (!Utils.checkType(type)) {
							return Stream.empty();
						}
						return Stream.of(TypeContextEntry.create(type));
					} else if (binding.getJavaElement() instanceof final ILocalVariable localVariable) {
						final IType type = localVariable.getJavaProject().findType(Signature.toString(localVariable.getTypeSignature()));
						if (!Utils.checkType(type)) {
							return Stream.empty();
						}
						final String fullyQualifiedName = type.getFullyQualifiedName();
						if (AiCoderPreferences.isIgnoreJreClasses() && JdkUtils.isJREPackage(fullyQualifiedName)) {
							return Stream.empty();
						}
						return Stream.of(TypeContextEntry.create(type));
					} else {
						AiCoderActivator.log().info("Skip binding: " + binding.getKey() + "/" +
								(binding.getJavaElement() != null ? binding.getJavaElement().getClass().getName() : "-"));
						return Stream.empty();
					}
				}))
				.toList();

		return new ScopeContextEntry(entries, Duration.ofMillis(System.currentTimeMillis() - before));
	}

	private static CompilationUnit parseUnit(ICompilationUnit unit) {
		final ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(unit);
		parser.setResolveBindings(true);
		return (CompilationUnit) parser.createAST(null);
	}

	@SuppressWarnings("restriction")
	private static List<IBinding> getBindingsInScope(CompilationUnit compilationUnit, int position) {
		return Arrays.asList(new ScopeAnalyzer(compilationUnit).getDeclarationsInScope(position, ScopeAnalyzer.VARIABLES | ScopeAnalyzer.TYPES | ScopeAnalyzer.CHECK_VISIBILITY));
	}
}
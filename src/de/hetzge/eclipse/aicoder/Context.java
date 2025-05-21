package de.hetzge.eclipse.aicoder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.internal.corext.dom.ScopeAnalyzer;
import org.eclipse.jdt.ui.JavaElementLabels;

/*
 * TODO lazy create methods
 * TODO MINIMUM, EXTENDED, FULL levels
 * TODO show tokens of each entry
 */

public class Context {

	public static class TokenCounter {
		private final int maxTokenCount;
		private int tokenCount;
		private final StringBuilder builder;

		public TokenCounter(int maxTokenCount) {
			this.maxTokenCount = maxTokenCount;
			this.tokenCount = 0;
			this.builder = new StringBuilder();
		}

		public boolean canAdd(ContextEntry entry) {
			final int beforeLength = this.builder.length();
			final int beforeTokenCount = this.tokenCount;
			count(entry);
			final int afterTokenCount = this.tokenCount;
			if (afterTokenCount > this.maxTokenCount) {
				this.builder.setLength(beforeLength);
				this.tokenCount = beforeTokenCount;
				return false;
			}
			return true;
		}

		public int count(ContextEntry entry) {
			final int beforeLength = this.builder.length();
			entry.apply(this.builder, 0, new TokenCounter(Integer.MAX_VALUE)); // TODO
			final int afterLength = this.builder.length();
			final int newTokensCount = Utils.countApproximateTokens(this.builder.substring(beforeLength, afterLength));
			this.tokenCount += newTokensCount;
			return newTokensCount;
		}

		public int countWithReset(ContextEntry entry) {
			reset();
			return count(entry);
		}

		public void reset() {
			this.tokenCount = 0;
			this.builder.setLength(0);
		}

		public static int countTokens(ContextEntry entry) {
			return new TokenCounter(Integer.MAX_VALUE).count(entry);
		}
	}

	public interface ContextEntry {
		void apply(StringBuilder builder, int level, TokenCounter counter);
	}

	public static abstract class BaseContextEntry implements ContextEntry {
		protected final List<? extends ContextEntry> childContextEntries;

		public BaseContextEntry(List<? extends ContextEntry> childContextEntries2) {
			this.childContextEntries = childContextEntries2;
		}

		@Override
		public void apply(StringBuilder builder, int level, TokenCounter counter) {
			for (final ContextEntry entry : this.childContextEntries) {
				if (counter.canAdd(entry)) {
					entry.apply(builder, level, counter);
				}
			}
		}
	}

	public static class ScopeContextEntry extends BaseContextEntry {

		public ScopeContextEntry(List<? extends ContextEntry> childContextEntries) {
			super(childContextEntries);
		}

		public static ScopeContextEntry create(ICompilationUnit unit, int offset) {
			final List<TypeContextEntry> entries = new ArrayList<>();
			for (final IBinding binding : getBindingsInScope(parseUnit(unit), offset)) {
				if (binding.getJavaElement() instanceof final IType type) {
					entries.add(TypeContextEntry.create(type));
				}
			}
			return new ScopeContextEntry(entries);
		}

		private static CompilationUnit parseUnit(ICompilationUnit unit) {
			final ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
			parser.setKind(ASTParser.K_COMPILATION_UNIT);
			parser.setSource(unit);
			parser.setResolveBindings(true);
			final CompilationUnit parsedUnit = (CompilationUnit) parser.createAST(null);
			return parsedUnit;
		}

		@SuppressWarnings("restriction")
		private static List<IBinding> getBindingsInScope(CompilationUnit compilationUnit, int position) {
			final ScopeAnalyzer analyzer = new ScopeAnalyzer(compilationUnit);
			final IBinding[] bindings = analyzer.getDeclarationsInScope(position, ScopeAnalyzer.VARIABLES | ScopeAnalyzer.TYPES | ScopeAnalyzer.CHECK_VISIBILITY);
			return Arrays.asList(bindings);
		}
	}

	public static class CurrentPackageContextEntry extends BaseContextEntry {

		public CurrentPackageContextEntry(List<? extends ContextEntry> entries) {
			super(entries);
		}

		public static CurrentPackageContextEntry create(ICompilationUnit unit) throws JavaModelException {
			final IPackageFragment packageFragment = (IPackageFragment) unit.getAncestor(IJavaElement.PACKAGE_FRAGMENT);
			return new CurrentPackageContextEntry(Arrays.stream(packageFragment.getCompilationUnits())
					.flatMap(LambdaExceptionUtils.rethrowFunction(it -> Arrays.stream(it.getAllTypes())))
					.map(TypeContextEntry::create)
					.toList());
		}
	}

	public static class ImportsContextEntry extends BaseContextEntry {
		public ImportsContextEntry(List<TypeContextEntry> childContextEntries) {
			super(childContextEntries);
		}

		public static ImportsContextEntry create(ICompilationUnit unit) throws JavaModelException {
			final List<TypeContextEntry> entries = new ArrayList<>();
			for (final IImportDeclaration importDeclaration : unit.getImports()) {
				final String elementName = importDeclaration.getElementName();
				try {
					if (!importDeclaration.isOnDemand()) {
						final IType type = unit.getJavaProject().findType(elementName);
						entries.add(TypeContextEntry.create(type));
					}
				} catch (final JavaModelException exception) {
					AiCoderActivator.getDefault().getLog().error("Failed to resolve import: " + elementName, exception);
				}
			}
			return new ImportsContextEntry(entries);
		}
	}

	public static class ClipboardContextEntry implements ContextEntry {
		@Override
		public void apply(StringBuilder builder, int level, TokenCounter counter) {
			// TODO
		}

		public static ClipboardContextEntry create() {
			return new ClipboardContextEntry();
		}
	}

	public static class RootContextEntry extends BaseContextEntry {
		public RootContextEntry(List<? extends ContextEntry> childContextEntries) {
			super(childContextEntries);
		}

		public static RootContextEntry create(ICompilationUnit unit, int offset) throws JavaModelException {
			return new RootContextEntry(List.of(
					ScopeContextEntry.create(unit, offset),
					ImportsContextEntry.create(unit),
					CurrentPackageContextEntry.create(unit),
					ClipboardContextEntry.create()));
		}
	}

	public static class TypeMemberContextEntry implements ContextEntry {
		private final String signature;
		private final String javadoc;

		private TypeMemberContextEntry(String signature, String javadoc) {
			this.signature = signature;
			this.javadoc = javadoc;
		}

		@Override
		public void apply(StringBuilder builder, int level, TokenCounter counter) {
//			if (this.javadoc != null) {
//				builder.append(this.javadoc);
//			}
			builder.append(this.signature).append("\n");
		}

		public static TypeMemberContextEntry create(IJavaElement element) throws JavaModelException {
			if (element instanceof final IField field) {
				final String signature = (Flags.isStatic(field.getFlags()) ? "static " : "") + JavaElementLabels.getElementLabel(field, JavaElementLabels.ALL_DEFAULT | JavaElementLabels.F_PRE_TYPE_SIGNATURE);
				final String javadoc = EclipseUtils.getJavadoc(field);
				return new TypeMemberContextEntry(signature, javadoc);
			} else if (element instanceof final IMethod method) {
				final String signature = (Flags.isStatic(method.getFlags()) ? "static " : "") + JavaElementLabels.getElementLabel(method, JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.M_PARAMETER_NAMES | JavaElementLabels.M_PRE_RETURNTYPE | JavaElementLabels.M_EXCEPTIONS);
				final String javadoc = EclipseUtils.getJavadoc(method);
				return new TypeMemberContextEntry(signature, javadoc);
			} else {
				throw new IllegalStateException(String.format("IJavaElement with type %s is not supported", element.getClass().getSimpleName()));
			}
		}
	}

	public static class TypeContextEntry implements ContextEntry {
		private final String signature;
		private final List<TypeMemberContextEntry> memberEntries;

		private TypeContextEntry(String signature, List<TypeMemberContextEntry> members) {
			this.signature = signature;
			this.memberEntries = members;
		}

		@Override
		public void apply(StringBuilder builder, int level, TokenCounter counter) {
			builder.append(this.signature).append("{\n");
			for (final TypeMemberContextEntry memberEntry : this.memberEntries) {
				if (counter.canAdd(memberEntry)) {
					memberEntry.apply(builder, level, counter);
				}
			}
			builder.append("}\n");
		}

		public static TypeContextEntry create(IType type) {
			final List<TypeMemberContextEntry> members = new ArrayList<>();
			try {
				final String typeSignature = Utils.getTypeKeywordLabel(type) + " " + JavaElementLabels.getElementLabel(type, JavaElementLabels.T_FULLY_QUALIFIED);
				for (final IField field : type.getFields()) {
					if (!Flags.isPrivate(field.getFlags())) {
						members.add(TypeMemberContextEntry.create(field));
					}
				}
				for (final IMethod method : type.getMethods()) {
					if (!Flags.isPrivate(method.getFlags())) {
						members.add(TypeMemberContextEntry.create(method));
					}
				}
				return new TypeContextEntry(typeSignature, members);
			} catch (final JavaModelException exception) {
				throw new RuntimeException("Failed to expand type context entry", exception);
			}
		}
	}
}

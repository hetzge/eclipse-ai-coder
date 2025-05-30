package de.hetzge.eclipse.aicoder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.internal.corext.dom.ScopeAnalyzer;
import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

/*
 * TODO lazy create methods
 * TODO Prio levels (High, Medium, Low)
 * TODO show tokens of each entry
 * TODO last edited files
 * TODO last viewed files
 * TODO manually ranked files
 * TODO sticky types
 * TODO ignore items (ignore-file and storage)
 * TODO prevent duplicates
 * TODO prevent jdk
 * TODO add java/maven metadata (Java Version, Dependencies)
 * TODO prefix/suffix context entry
 */

public final class Context {

	public static class TokenCounter {
		private final int maxTokenCount;
		private int tokenCount;
		private final StringBuilder builder;
		private final Set<String> allKeys; // used to prevent duplicates

		public TokenCounter(int maxTokenCount) {
			this.maxTokenCount = maxTokenCount;
			this.tokenCount = 0;
			this.builder = new StringBuilder();
			this.allKeys = new HashSet<>();
		}

		public boolean canAdd(ContextEntry entry) {
			if (this.allKeys.contains(entry.getKey())) {
				return false;
			}
			final int beforeLength = this.builder.length();
			final int beforeTokenCount = this.tokenCount;
			count(entry);
			final int afterTokenCount = this.tokenCount;
			if (afterTokenCount > this.maxTokenCount) {
				this.builder.setLength(beforeLength);
				this.tokenCount = beforeTokenCount;
				return false;
			}
			this.allKeys.add(entry.getKey());
			return true;
		}

		public int count(ContextEntry entry) {
			final int beforeLength = this.builder.length();
			entry.apply(this.builder, new TokenCounter(Integer.MAX_VALUE)); // TODO
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

	public static abstract class ContextEntry {

		private int tokenCount;
		protected final List<? extends ContextEntry> childContextEntries;

		public ContextEntry(List<? extends ContextEntry> childContextEntries) {
			this.childContextEntries = childContextEntries;
		}

		public int getTokenCount() {
			return this.tokenCount;
		}

		public void setTokenCount(int tokenCount) {
			this.tokenCount = tokenCount;
		}

		public abstract String getKey();

		public void apply(StringBuilder builder, TokenCounter counter) {
			for (final ContextEntry entry : this.childContextEntries) {
				apply(builder, counter, entry);
			}
		}

		public List<? extends ContextEntry> getChildContextEntries() {
			return this.childContextEntries;
		}

		abstract String getLabel();

		public Image getImage() {
			return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_ELEMENT);
		}

		public static void apply(StringBuilder builder, TokenCounter counter, final ContextEntry entry) {
			final int beforeTokenCount = counter.tokenCount;
			if (counter.canAdd(entry)) {
				entry.apply(builder, counter);
				entry.setTokenCount(counter.tokenCount - beforeTokenCount);
			}
		}
	}

	public static class EmptyContextEntry extends ContextEntry {

		public EmptyContextEntry() {
			super(List.of());
		}

		@Override
		public String getLabel() {
			return "Empty";
		}

		@Override
		public String getKey() {
			return "EMPTY";
		}
	}

	public static class ScopeContextEntry extends ContextEntry {

		public ScopeContextEntry(List<? extends ContextEntry> childContextEntries) {
			super(childContextEntries);
		}

		@Override
		public String getLabel() {
			return "Scope";
		}

		@Override
		public String getKey() {
			return "SCOPE";
		}

		@Override
		public Image getImage() {
			return AiCoderActivator.getImage(AiCoderImageKey.SCOPE_ICON);
		}

		public static ScopeContextEntry create(ICompilationUnit unit, int offset) throws JavaModelException {
			final List<TypeContextEntry> entries = new ArrayList<>();
			final CompilationUnit parsedUnit = parseUnit(unit);
			for (final IBinding binding : getBindingsInScope(parsedUnit, offset)) {
				if (binding.getJavaElement() instanceof final IType type) {
					entries.add(TypeContextEntry.create(type));
				} else if (binding.getJavaElement() instanceof final ILocalVariable localVariable) {
					final IType type = localVariable.getJavaProject().findType(Signature.toString(localVariable.getTypeSignature()));
					if (type != null) {
						entries.add(TypeContextEntry.create(type));
					}
				} else {
					System.out.println("Context.ScopeContextEntry.create() " + binding.getJavaElement().getClass());
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
			return Arrays.asList(new ScopeAnalyzer(compilationUnit).getDeclarationsInScope(position, ScopeAnalyzer.VARIABLES | ScopeAnalyzer.TYPES | ScopeAnalyzer.CHECK_VISIBILITY));
		}
	}

	public static class PrefixContextEntry extends ContextEntry {
		private final String content;

		private PrefixContextEntry(String content) {
			super(List.of());
			this.content = content;
		}

		@Override
		public String getKey() {
			return "PREFIX_" + this.content.hashCode();
		}

		@Override
		String getLabel() {
			return "Prefix";
		}

		@Override
		public Image getImage() {
			return AiCoderActivator.getImage(AiCoderImageKey.BEFORE_ICON);
		}

		@Override
		public void apply(StringBuilder builder, TokenCounter counter) {
			builder.append(this.content);
		}

		public static PrefixContextEntry create(IDocument document, int modelOffset) throws BadLocationException {
			final int modelLine = document.getLineOfOffset(modelOffset);
			final int firstLine = Math.max(0, modelLine - 100); // TODO
			final String prefix = document.get(document.getLineOffset(firstLine), modelOffset - document.getLineOffset(firstLine));
			return new PrefixContextEntry(prefix);
		}
	}

	public static class SuffixContextEntry extends ContextEntry {
		public static final String FILL_HERE_PLACEHOLDER = "<<FILL_HERE>>";
		private final String content;

		public SuffixContextEntry(String content) {
			super(List.of());
			this.content = content;
		}

		@Override
		public String getKey() {
			return "SUFFIX_" + this.content.hashCode();
		}

		@Override
		String getLabel() {
			return "Suffix";
		}

		@Override
		public Image getImage() {
			return AiCoderActivator.getImage(AiCoderImageKey.AFTER_ICON);
		}

		@Override
		public void apply(StringBuilder builder, TokenCounter counter) {
			builder.append(FILL_HERE_PLACEHOLDER);
			builder.append(this.content);
		}

		public static SuffixContextEntry create(IDocument document, int modelOffset) throws BadLocationException {
			final int modelLine = document.getLineOfOffset(modelOffset);
			final int lastLine = Math.min(document.getNumberOfLines() - 1, modelLine + 100); // TODO
			final String suffix = document.get(modelOffset, document.getLineOffset(lastLine) - modelOffset);
			return new SuffixContextEntry(suffix);
		}
	}

	public static class CurrentPackageContextEntry extends ContextEntry {

		private final String name;

		public CurrentPackageContextEntry(String name, List<? extends ContextEntry> entries) {
			super(entries);
			this.name = name;
		}

		@Override
		public String getLabel() {
			return this.name;
		}

		@Override
		public String getKey() {
			return "PACKAGE_" + this.name;
		}

		@Override
		public Image getImage() {
			return AiCoderActivator.getImage(AiCoderImageKey.PACKAGE_ICON);
		}

		public static CurrentPackageContextEntry create(ICompilationUnit unit) throws JavaModelException {
			final IPackageFragment packageFragment = (IPackageFragment) unit.getAncestor(IJavaElement.PACKAGE_FRAGMENT);
			return new CurrentPackageContextEntry(packageFragment.getElementName(), Arrays.stream(packageFragment.getCompilationUnits())
					.flatMap(LambdaExceptionUtils.rethrowFunction(it -> Arrays.stream(it.getAllTypes())))
					.map(TypeContextEntry::create)
					.toList());
		}
	}

	public static class ImportsContextEntry extends ContextEntry {
		public ImportsContextEntry(List<TypeContextEntry> childContextEntries) {
			super(childContextEntries);
		}

		@Override
		public String getLabel() {
			return "Imports";
		}

		@Override
		public String getKey() {
			return "IMPORTS";
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

	public static class ClipboardContextEntry extends ContextEntry {

		public ClipboardContextEntry() {
			super(List.of());
		}

		@Override
		public void apply(StringBuilder builder, TokenCounter counter) {
			// TODO
		}

		@Override
		public String getLabel() {
			return "Clipboard";
		}

		@Override
		public String getKey() {
			return "CLIPBOARD";
		}

		@Override
		public Image getImage() {
			return AiCoderActivator.getImage(AiCoderImageKey.COPY_ICON);
		}

		@Override
		public List<? extends ContextEntry> getChildContextEntries() {
			return List.of();
		}

		public static ClipboardContextEntry create() {
			return new ClipboardContextEntry();
		}
	}

	public static class RootContextEntry extends ContextEntry {
		public RootContextEntry(List<? extends ContextEntry> childContextEntries) {
			super(childContextEntries);
		}

		@Override
		public String getLabel() {
			return "Root";
		}

		@Override
		public String getKey() {
			return "ROOT";
		}

		public static RootContextEntry create(IDocument document, ICompilationUnit unit, int offset) throws JavaModelException, BadLocationException {
			return new RootContextEntry(List.of(
					ScopeContextEntry.create(unit, offset),
					ImportsContextEntry.create(unit),
					CurrentPackageContextEntry.create(unit),
					ClipboardContextEntry.create(),
					PrefixContextEntry.create(document, offset),
					SuffixContextEntry.create(document, offset)));
		}
	}

	public static class TypeMemberContextEntry extends ContextEntry {
		private final IJavaElement element;
		private final String signature;
		private final String javadoc; // TODO javadoc as child

		private TypeMemberContextEntry(IJavaElement element, String signature, String javadoc) {
			super(List.of());
			this.element = element;
			this.signature = signature;
			this.javadoc = javadoc;
		}

		@Override
		public String getLabel() {
			return this.signature;
		}

		@Override
		public String getKey() {
			return this.signature;
		}

		@Override
		public List<? extends ContextEntry> getChildContextEntries() {
			return List.of();
		}

		@Override
		public void apply(StringBuilder builder, TokenCounter counter) {
//			if (this.javadoc != null) {
//				builder.append(this.javadoc);
//			}
			builder.append(this.signature).append("\n");
		}

		public static TypeMemberContextEntry create(IJavaElement element) throws JavaModelException {
			if (element instanceof final IField field) {
				final String signature = (Flags.isStatic(field.getFlags()) ? "static " : "") + JavaElementLabels.getElementLabel(field, JavaElementLabels.ALL_DEFAULT | JavaElementLabels.F_PRE_TYPE_SIGNATURE);
				final String javadoc = EclipseUtils.getJavadoc(field);
				return new TypeMemberContextEntry(element, signature, javadoc);
			} else if (element instanceof final IMethod method) {
				final String signature = (Flags.isStatic(method.getFlags()) ? "static " : "") + JavaElementLabels.getElementLabel(method, JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.M_PARAMETER_NAMES | JavaElementLabels.M_PRE_RETURNTYPE | JavaElementLabels.M_EXCEPTIONS);
				final String javadoc = EclipseUtils.getJavadoc(method);
				return new TypeMemberContextEntry(element, signature, javadoc);
			} else {
				throw new IllegalStateException(String.format("IJavaElement with type %s is not supported", element.getClass().getSimpleName()));
			}
		}
	}

	public static class TypeContextEntry extends ContextEntry {
		private final String signature;

		private TypeContextEntry(String signature, List<TypeMemberContextEntry> members) {
			super(members);
			this.signature = signature;
		}

		@Override
		public String getLabel() {
			return this.signature;
		}

		@Override
		public String getKey() {
			return this.signature;
		}

		@Override
		public Image getImage() {
			return AiCoderActivator.getImage(AiCoderImageKey.TYPE_ICON);
		}

		@Override
		public void apply(StringBuilder builder, TokenCounter counter) {
			builder.append(this.signature).append("{\n");
			super.apply(builder, counter);
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

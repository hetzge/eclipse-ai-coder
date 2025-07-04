package de.hetzge.eclipse.aicoder;

import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
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
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import mjson.Json;

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
 * TODO mark duplicates
 * TODO prevent jdk
 * TODO add java/maven metadata (Java Version, Dependencies)
 * TODO custom context
 * TODO last visited files
 * TODO https://github.com/continuedev/continue/blob/main/core/autocomplete/postprocessing/index.ts
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

	public static record ContextEntryKey(
			String prefix,
			String value) {

		public String getKeyString() {
			return String.format("%s::%s", this.prefix, Base64.getEncoder().encodeToString(this.value.getBytes(StandardCharsets.UTF_8)));
		}

		public static Optional<ContextEntryKey> parseKeyString(String keyString) {
			final String[] parts = keyString.split("::");
			if (parts.length != 2) {
				return Optional.empty();
			}
			return Optional.of(new ContextEntryKey(parts[0], new String(Base64.getDecoder().decode(parts[1]), StandardCharsets.UTF_8)));
		}
	}

	public static abstract class ContextEntry {

		private int tokenCount;
		protected final List<? extends ContextEntry> childContextEntries;
		private final Duration creationDuration;

		public ContextEntry(List<? extends ContextEntry> childContextEntries, Duration creationDuration) {
			this.tokenCount = 0;
			this.childContextEntries = childContextEntries;
			this.creationDuration = creationDuration;
		}

		public int getTokenCount() {
			return this.tokenCount;
		}

		public void setTokenCount(int tokenCount) {
			this.tokenCount = tokenCount;
		}

		public abstract ContextEntryKey getKey();

		public String getContent() {
			return this.childContextEntries.stream()
					.map(ContextEntry::apply)
					// not using "\n" here because prefix/suffix should not be separated by line break
					.collect(Collectors.joining());
		}

		public List<? extends ContextEntry> getChildContextEntries() {
			return this.childContextEntries;
		}

		abstract String getLabel();

		public Image getImage() {
			return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_ELEMENT);
		}

		public Duration getCreationDuration() {
			return this.creationDuration;
		}

		public static String apply(final ContextEntry entry) {
			if (!ContextPreferences.isBlacklisted(entry.getKey())) { // TODO prevent duplication
				final String content = entry.getContent();
				entry.setTokenCount(content.length());
				return content;
			} else {
				entry.setTokenCount(0);
				return "";
			}
		}
	}

	public static class EmptyContextEntry extends ContextEntry {

		public static final String PREFIX = "EMPTY";

		public EmptyContextEntry() {
			super(List.of(), Duration.ZERO);
		}

		@Override
		public String getLabel() {
			return "Empty";
		}

		@Override
		public ContextEntryKey getKey() {
			return new ContextEntryKey(PREFIX, PREFIX);
		}
	}

	public static class ScopeContextEntry extends ContextEntry {

		public static final String PREFIX = "SCOPE";

		public ScopeContextEntry(List<? extends ContextEntry> childContextEntries, Duration creationDuration) {
			super(childContextEntries, creationDuration);
		}

		@Override
		public String getLabel() {
			return "Scope";
		}

		@Override
		public String getContent() {
			return super.getContent() + "\n";
		}

		@Override
		public ContextEntryKey getKey() {
			return new ContextEntryKey(PREFIX, PREFIX);
		}

		@Override
		public Image getImage() {
			return AiCoderActivator.getImage(AiCoderImageKey.SCOPE_ICON);
		}

		public static ScopeContextEntry create(ICompilationUnit unit, int offset) throws CoreException {
			final long before = System.currentTimeMillis();
			final List<TypeContextEntry> entries = new ArrayList<>();
			final CompilationUnit parsedUnit = parseUnit(unit);
			for (final IBinding binding : getBindingsInScope(parsedUnit, offset)) {
				if (binding.getJavaElement() instanceof final IType type) {
					if (Utils.checkType(type)) {
						entries.add(TypeContextEntry.create(type));
					}
				} else if (binding.getJavaElement() instanceof final ILocalVariable localVariable) {
					final IType type = localVariable.getJavaProject().findType(Signature.toString(localVariable.getTypeSignature()));
					if (type != null) {
						entries.add(TypeContextEntry.create(type));
					}
				} else {
					AiCoderActivator.log().info("Skip binding: " + binding.getKey() + "/" + (binding.getJavaElement() != null ? binding.getJavaElement().getClass().getName() : "-"));
				}
			}
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

	public static class PrefixContextEntry extends ContextEntry {
		public static final String PREFIX = "PREFIX";

		private final String filename;
		private final String content;

		private PrefixContextEntry(String filename, String content, Duration creationDuration) {
			super(List.of(), creationDuration);
			this.filename = filename;
			this.content = content;
		}

		@Override
		public ContextEntryKey getKey() {
			return new ContextEntryKey(PREFIX, PREFIX);
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
		public String getContent() {
			return String.format("File: %s\n%s", this.filename, this.content);
		}

		public static PrefixContextEntry create(String filename, IDocument document, int modelOffset) throws BadLocationException {
			final long before = System.currentTimeMillis();
			final int modelLine = document.getLineOfOffset(modelOffset);
			final int maxLines = AiCoderPreferences.getMaxPrefixSize();
			final int firstLine = Math.max(0, modelLine - maxLines);
			final String prefix = document.get(document.getLineOffset(firstLine), modelOffset - document.getLineOffset(firstLine));
			return new PrefixContextEntry(filename, prefix, Duration.ofMillis(System.currentTimeMillis() - before));
		}
	}

	public static class SuffixContextEntry extends ContextEntry {
		public static final String FILL_HERE_PLACEHOLDER = "<<FILL_HERE>>";
		private final String content;

		public static final String PREFIX = "SUFFIX";

		public SuffixContextEntry(String content, Duration creationDuration) {
			super(List.of(), creationDuration);
			this.content = content;
		}

		@Override
		public ContextEntryKey getKey() {
			return new ContextEntryKey(PREFIX, PREFIX);
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
		public String getContent() {
			return FILL_HERE_PLACEHOLDER + this.content + "\n";
		}

		public static SuffixContextEntry create(IDocument document, int modelOffset) throws BadLocationException {
			final long before = System.currentTimeMillis();
			final String suffix = document.get(modelOffset, document.getLength() - modelOffset);
			return new SuffixContextEntry(suffix, Duration.ofMillis(System.currentTimeMillis() - before));
		}
	}

	public static class PackageContextEntry extends ContextEntry {

		public static final String PREFIX = "PACKAGE";

		private final String name;

		public PackageContextEntry(String name, List<? extends ContextEntry> entries, Duration creationDuration) {
			super(entries, creationDuration);
			this.name = name;
		}

		@Override
		public String getLabel() {
			return this.name;
		}

		@Override
		public String getContent() {
			return super.getContent() + "\n";
		}

		@Override
		public ContextEntryKey getKey() {
			return new ContextEntryKey(PREFIX, this.name);
		}

		@Override
		public Image getImage() {
			return AiCoderActivator.getImage(AiCoderImageKey.PACKAGE_ICON);
		}

		public static PackageContextEntry create(ICompilationUnit unit) throws CoreException {
			return create((IPackageFragment) unit.getAncestor(IJavaElement.PACKAGE_FRAGMENT));
		}

		public static Optional<PackageContextEntry> create(ContextEntryKey key) throws JavaModelException, CoreException {
			if (!key.prefix().equals(PREFIX)) {
				return Optional.empty();
			}
			// Require java project
			final Optional<IJavaProject> javaProjectOptional = JavaProjectUtils.getCurrentJavaProject();
			if (javaProjectOptional.isEmpty()) {
				return Optional.empty();
			}
			final IJavaProject javaProject = javaProjectOptional.get();
			// Require package fragment
			final Optional<IPackageFragment> packageOptional = JavaProjectUtils.findPackageFragment(javaProject, key.value());
			if (packageOptional.isEmpty()) {
				return Optional.empty();
			}
			final IPackageFragment packageFragment = packageOptional.get();
			return Optional.of(create(packageFragment));
		}

		private static PackageContextEntry create(final IPackageFragment packageFragment) throws CoreException {
			final long before = System.currentTimeMillis();
			final String elementName = packageFragment.getElementName();
			final List<TypeContextEntry> entries = Arrays.stream(packageFragment.getCompilationUnits())
					.flatMap(LambdaExceptionUtils.rethrowFunction(it -> Arrays.stream(it.getAllTypes())))
					.filter(Utils::checkType)
					.map(LambdaExceptionUtils.rethrowFunction(TypeContextEntry::create))
					.toList();
			return new PackageContextEntry(elementName, entries, Duration.ofMillis(System.currentTimeMillis() - before));
		}
	}

	public static class ImportsContextEntry extends ContextEntry {
		public static final String PREFIX = "IMPORTS";

		public ImportsContextEntry(List<TypeContextEntry> childContextEntries, Duration creationDuration) {
			super(childContextEntries, creationDuration);
		}

		@Override
		public String getLabel() {
			return "Imports";
		}

		@Override
		public String getContent() {
			return super.getContent() + "\n";
		}

		@Override
		public Image getImage() {
			return AiCoderActivator.getImage(AiCoderImageKey.IMPORT_ICON);
		}

		@Override
		public ContextEntryKey getKey() {
			return new ContextEntryKey(PREFIX, PREFIX);
		}

		public static ImportsContextEntry create(ICompilationUnit unit) throws CoreException {
			final long before = System.currentTimeMillis();
			final List<TypeContextEntry> entries = new ArrayList<>();
			for (final IImportDeclaration importDeclaration : unit.getImports()) {
				final String elementName = importDeclaration.getElementName();
				if (!importDeclaration.isOnDemand()) {
					final IType type = unit.getJavaProject().findType(elementName);
					if (Utils.checkType(type)) {
						entries.add(TypeContextEntry.create(type));
					}
				}
			}
			return new ImportsContextEntry(entries, Duration.ofMillis(System.currentTimeMillis() - before));
		}
	}

	public static class ClipboardContextEntry extends ContextEntry {

		public static final String PREFIX = "CLIPBOARD";

		private final String content;

		public ClipboardContextEntry(String content, Duration creationDuration) {
			super(List.of(), creationDuration);
			this.content = content;
		}

		@Override
		public String getContent() {
			return "Clipboard content:\n" + this.content + "\n\n";
		}

		@Override
		public String getLabel() {
			return "Clipboard";
		}

		@Override
		public ContextEntryKey getKey() {
			return new ContextEntryKey(PREFIX, PREFIX);
		}

		@Override
		public Image getImage() {
			return AiCoderActivator.getImage(AiCoderImageKey.COPY_ICON);
		}

		@Override
		public List<? extends ContextEntry> getChildContextEntries() {
			return List.of();
		}

		public static ClipboardContextEntry create() throws UnsupportedFlavorException, IOException {
			final long before = System.currentTimeMillis();
			return Display.getDefault().syncCall(() -> {
				final String clipboardContent = (String) new Clipboard(Display.getDefault()).getContents(TextTransfer.getInstance());
				return new ClipboardContextEntry(clipboardContent != null ? clipboardContent : "", Duration.ofMillis(System.currentTimeMillis() - before));
			});
		}
	}

	public static class RootContextEntry extends ContextEntry {
		public static final String PREFIX = "ROOT";

		public RootContextEntry(List<? extends ContextEntry> childContextEntries, Duration creationDuration) {
			super(childContextEntries, creationDuration);
		}

		@Override
		public String getLabel() {
			return "Root";
		}

		@Override
		public String getContent() {
			return super.getContent() + "\n";
		}

		@Override
		public ContextEntryKey getKey() {
			return new ContextEntryKey(PREFIX, "ROOT");
		}

		public static RootContextEntry create(IDocument document, IEditorInput editorInput, int offset) throws BadLocationException, UnsupportedFlavorException, IOException, CoreException {
			final String filename = EclipseUtils.getFilename(editorInput).orElse("Active File");
			final long before = System.currentTimeMillis();
			final Optional<ICompilationUnit> compilationUnitOptional = EclipseUtils.getCompilationUnit(editorInput);
			final List<ContextEntry> entries = new ArrayList<>();
			entries.add(StickyContextEntry.create());
			entries.add(UserContextEntry.create());
			if (compilationUnitOptional.isPresent()) {
				final ICompilationUnit unit = compilationUnitOptional.get();
				entries.add(ScopeContextEntry.create(unit, offset));
				entries.add(ImportsContextEntry.create(unit));
				entries.add(PackageContextEntry.create(unit));
			}
			entries.add(ClipboardContextEntry.create());
			entries.add(PrefixContextEntry.create(filename, document, offset));
			entries.add(SuffixContextEntry.create(document, offset));
			entries.add(BlacklistedContextEntry.create());
			return new RootContextEntry(entries, Duration.ofMillis(System.currentTimeMillis() - before));
		}
	}

	public static class TypeMemberContextEntry extends ContextEntry {
		public static final String PREFIX = "TYPE_MEMBER";

		private final IJavaElement element;
		private final String signature;
		private final String javadoc; // TODO javadoc as child

		private TypeMemberContextEntry(IJavaElement element, String signature, String javadoc, Duration creationDuration) {
			super(List.of(), creationDuration);
			this.element = element;
			this.signature = signature;
			this.javadoc = javadoc;
		}

		@Override
		public String getLabel() {
			return this.signature;
		}

		@Override
		public ContextEntryKey getKey() {
			return new ContextEntryKey(PREFIX, this.element.getHandleIdentifier());
		}

		@Override
		public List<? extends ContextEntry> getChildContextEntries() {
			return List.of();
		}

		@Override
		public String getContent() {
			// TODO javadoc
			return String.format("  %s;\n", this.signature);
		}

		public static TypeMemberContextEntry create(IJavaElement element) throws CoreException {
			final long before = System.currentTimeMillis();
			if (element instanceof final IField field) {
				final String signature = (Flags.isStatic(field.getFlags()) ? "static " : "") + JavaElementLabels.getElementLabel(field, JavaElementLabels.ALL_DEFAULT | JavaElementLabels.F_PRE_TYPE_SIGNATURE);
				final String javadoc = EclipseUtils.getJavadoc(field);
				return new TypeMemberContextEntry(element, signature, javadoc, Duration.ofMillis(System.currentTimeMillis() - before));
			} else if (element instanceof final IMethod method) {
				final String signature = (Flags.isStatic(method.getFlags()) ? "static " : "") + JavaElementLabels.getElementLabel(method, JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.M_PARAMETER_NAMES | JavaElementLabels.M_PRE_RETURNTYPE | JavaElementLabels.M_EXCEPTIONS);
				final String javadoc = EclipseUtils.getJavadoc(method);
				return new TypeMemberContextEntry(element, signature, javadoc, Duration.ofMillis(System.currentTimeMillis() - before));
			} else {
				throw new IllegalStateException(String.format("IJavaElement with type %s is not supported", element.getClass().getSimpleName()));
			}
		}

		public static Optional<TypeMemberContextEntry> create(ContextEntryKey key) throws CoreException {
			if (!key.prefix().equals(PREFIX)) {
				return Optional.empty();
			}
			return Optional.ofNullable(JavaCore.create(key.value()))
					.map(LambdaExceptionUtils.rethrowFunction(TypeMemberContextEntry::create));
		}
	}

	public static class TypeContextEntry extends ContextEntry {
		public static final String PREFIX = "TYPE";

		private final IType type;
		private final String signature;

		private TypeContextEntry(IType type, String signature, List<TypeMemberContextEntry> members, Duration creationDuration) {
			super(members, creationDuration);
			this.type = type;
			this.signature = signature;
		}

		@Override
		public String getLabel() {
			return this.signature;
		}

		@Override
		public ContextEntryKey getKey() {
			return new ContextEntryKey(PREFIX, this.type.getHandleIdentifier());
		}

		@Override
		public Image getImage() {
			return AiCoderActivator.getImage(AiCoderImageKey.TYPE_ICON);
		}

		@Override
		public String getContent() {
			return String.format("%s{\n%s\n}\n", this.signature, super.getContent());
		}

		public static TypeContextEntry create(IType type) throws CoreException {
			final long before = System.currentTimeMillis();
			final List<TypeMemberContextEntry> members = new ArrayList<>();
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
			return new TypeContextEntry(type, typeSignature, members, Duration.ofMillis(System.currentTimeMillis() - before));
		}

		public static Optional<TypeContextEntry> create(ContextEntryKey key) throws CoreException {
			if (!key.prefix().equals(PREFIX)) {
				return Optional.empty();
			}
			return Optional.ofNullable(JavaCore.create(key.value()))
					.filter(IType.class::isInstance)
					.map(IType.class::cast)
					.filter(Utils::checkType)
					.map(LambdaExceptionUtils.rethrowFunction(TypeContextEntry::create));
		}
	}

	public static class StickyContextEntry extends ContextEntry {

		private StickyContextEntry(List<? extends ContextEntry> childContextEntries, Duration creationDuration) {
			super(childContextEntries, creationDuration);
		}

		public static final String PREFIX = "STICKY";

		@Override
		public ContextEntryKey getKey() {
			return new ContextEntryKey(PREFIX, PREFIX);
		}

		@Override
		String getLabel() {
			return "Sticky";
		}

		@Override
		public String getContent() {
			return super.getContent() + "\n";
		}

		@Override
		public Image getImage() {
			return AiCoderActivator.getImage(AiCoderImageKey.PIN_ICON);
		}

		public static StickyContextEntry create() throws CoreException {
			final long before = System.currentTimeMillis();
			final List<? extends ContextEntry> entries = ContextPreferences.getStickylist().stream()
					.map(LambdaExceptionUtils.rethrowFunction(Context::create))
					.flatMap(Optional::stream)
					.toList();
			return new StickyContextEntry(entries, Duration.ofMillis(System.currentTimeMillis() - before));
		}
	}

	public static class BlacklistedContextEntry extends ContextEntry {
		public static final String PREFIX = "BLACKLISTED";

		private BlacklistedContextEntry(List<? extends ContextEntry> childContextEntries, Duration creationDuration) {
			super(childContextEntries, creationDuration);
		}

		@Override
		public ContextEntryKey getKey() {
			return new ContextEntryKey(PREFIX, PREFIX);
		}

		@Override
		String getLabel() {
			return "Blacklist";
		}

		@Override
		public String getContent() {
			return super.getContent() + "\n";
		}

		@Override
		public Image getImage() {
			return AiCoderActivator.getImage(AiCoderImageKey.BLACKLIST_ICON);
		}

		public static BlacklistedContextEntry create() throws CoreException {
			final long before = System.currentTimeMillis();
			final List<? extends ContextEntry> entries = ContextPreferences.getBlacklist().stream()
					.map(LambdaExceptionUtils.rethrowFunction(Context::create))
					.flatMap(Optional::stream)
					.toList();
			return new BlacklistedContextEntry(entries, Duration.ofMillis(System.currentTimeMillis() - before));
		}
	}

	public static class UserContextEntry extends ContextEntry {
		public static final String PREFIX = "USER";

		private UserContextEntry(List<CustomContextEntry> childContextEntries, Duration creationDuration) {
			super(childContextEntries, creationDuration);
		}

		@Override
		public ContextEntryKey getKey() {
			return new ContextEntryKey(PREFIX, PREFIX);
		}

		@Override
		String getLabel() {
			return "Custom";
		}

		@Override
		public String getContent() {
			return super.getContent() + "\n";
		}

		public static UserContextEntry create() {
			final long before = System.currentTimeMillis();
			final List<CustomContextEntry> entries = ContextPreferences.getCustomContextEntries();
			return new UserContextEntry(entries, Duration.ofMillis(System.currentTimeMillis() - before));
		}
	}

	public static class CustomContextEntry extends ContextEntry {
		public static final String PREFIX = "CUSTOM";

		private final List<CustomContextEntry> childContextEntries;
		private final UUID id;
		private final String title;
		private final String content;
		private final String glob;

		public CustomContextEntry(List<CustomContextEntry> childContextEntries, UUID id, String title, String content, String glob, Duration creationDuration) {
			super(childContextEntries, creationDuration);
			this.childContextEntries = childContextEntries;
			this.id = id;
			this.title = title;
			this.content = content;
			this.glob = glob;
		}

		@Override
		public String getContent() {
			return String.format("%s\n%s\n", this.content, super.getContent());
		}

		@Override
		public ContextEntryKey getKey() {
			return new ContextEntryKey(PREFIX, this.id.toString());
		}

		@Override
		String getLabel() {
			return this.title;
		}

		@Override
		public List<CustomContextEntry> getChildContextEntries() {
			return this.childContextEntries;
		}

		public UUID getId() {
			return this.id;
		}

		public String getTitle() {
			return this.title;
		}

		public String getGlob() {
			return this.glob;
		}

		public Json toJson() {
			return Json.object()
					.set("children", this.childContextEntries.stream().map(CustomContextEntry::toJson).toList())
					.set("id", this.id.toString())
					.set("title", this.title)
					.set("content", this.content)
					.set("glob", this.glob);
		}

		public static Optional<CustomContextEntry> create(ContextEntryKey key) {
			return ContextPreferences.getCustomContextEntries().stream().filter(it -> Objects.equals(it.getId().toString(), key.value())).findFirst();
		}

		public static CustomContextEntry createFromJson(Json json) {
			final long before = System.currentTimeMillis();
			final List<CustomContextEntry> childEntries = json.at("children").asJsonList().stream().map(CustomContextEntry::createFromJson).toList();
			final UUID id = UUID.fromString(json.at("id").asString());
			final String title = json.at("title").asString();
			final String content = json.at("content").asString();
			final String glob = json.at("glob").asString();
			return new CustomContextEntry(childEntries, id, title, content, glob, Duration.ofMillis(System.currentTimeMillis() - before));
		}
	}
}
package de.hetzge.eclipse.aicoder.context;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.JavaElementLabels;

import de.hetzge.eclipse.aicoder.util.EclipseUtils;
import de.hetzge.eclipse.aicoder.util.LambdaExceptionUtils;

public class TypeMemberContextEntry extends ContextEntry {
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
	public String getContent(ContextContext context) {
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
package de.hetzge.eclipse.aicoder.context;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.swt.graphics.Image;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.AiCoderImageKey;
import de.hetzge.eclipse.aicoder.LambdaExceptionUtils;
import de.hetzge.eclipse.aicoder.Utils;

public class TypeContextEntry extends ContextEntry {
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
	public String getContent(ContextContext context) {
		return String.format("%s{\n%s}\n", this.signature, super.getContent(context));
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
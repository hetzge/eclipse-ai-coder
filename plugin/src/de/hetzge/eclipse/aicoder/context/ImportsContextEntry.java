package de.hetzge.eclipse.aicoder.context;

import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.swt.graphics.Image;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.AiCoderImageKey;
import de.hetzge.eclipse.aicoder.preferences.AiCoderPreferences;
import de.hetzge.eclipse.aicoder.util.ContextUtils;
import de.hetzge.eclipse.aicoder.util.JdkUtils;
import de.hetzge.eclipse.aicoder.util.LambdaExceptionUtils;
import de.hetzge.eclipse.aicoder.util.Utils;

public class ImportsContextEntry extends ContextEntry {
	public static final String PREFIX = "IMPORTS";

	public ImportsContextEntry(List<TypeContextEntry> childContextEntries, Duration creationDuration) {
		super(childContextEntries, creationDuration);
	}

	@Override
	public String getLabel() {
		return "Imports";
	}

	@Override
	public String getContent(ContextContext context) {
		return ContextUtils.contentTemplate("Imported types", super.getContent(context));
	}

	@Override
	public Image getImage() {
		return AiCoderActivator.getImage(AiCoderImageKey.IMPORT_ICON);
	}

	@Override
	public ContextEntryKey getKey() {
		return new ContextEntryKey(PREFIX, PREFIX);
	}

	public static ContextEntryFactory factory(ICompilationUnit unit) {
		return new ContextEntryFactory(PREFIX, () -> create(unit));
	}

	public static ImportsContextEntry create(ICompilationUnit unit) throws CoreException {
		final long before = System.currentTimeMillis();
		final List<TypeContextEntry> entries = Stream.of(unit.getImports())
				.parallel()
				.filter(importDeclaration -> !importDeclaration.isOnDemand())
				.map(importDeclaration -> importDeclaration.getElementName())
				.filter(elementName -> !AiCoderPreferences.isIgnoreJreClasses() || !JdkUtils.isJREPackage(elementName))
				.map(LambdaExceptionUtils.rethrowFunction(unit.getJavaProject()::findType))
				.filter(Utils::checkType)
				.map(LambdaExceptionUtils.rethrowFunction(TypeContextEntry::create))
				.toList();
		return new ImportsContextEntry(entries, Duration.ofMillis(System.currentTimeMillis() - before));
	}
}
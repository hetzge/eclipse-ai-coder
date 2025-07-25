package de.hetzge.eclipse.aicoder.context;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.swt.graphics.Image;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.AiCoderImageKey;
import de.hetzge.eclipse.aicoder.util.JavaProjectUtils;
import de.hetzge.eclipse.aicoder.util.LambdaExceptionUtils;
import de.hetzge.eclipse.aicoder.util.Utils;

public class PackageContextEntry extends ContextEntry {

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
	public String getContent(ContextContext context) {
		return super.getContent(context) + "\n";
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
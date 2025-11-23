package de.hetzge.eclipse.aicoder.util;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;

import de.hetzge.eclipse.aicoder.AiCoderActivator;

public final class JdtUtils {
	private JdtUtils() {
	}

	public static ICompilationUnit[] getCompilationUnits(final IPackageFragment packageFragment) {
		try {
			return packageFragment.getCompilationUnits();
		} catch (final JavaModelException exception) {
			AiCoderActivator.log().error("Failed to get compilation units for package fragment: " + packageFragment, exception);
			return new ICompilationUnit[0];
		}
	}
}

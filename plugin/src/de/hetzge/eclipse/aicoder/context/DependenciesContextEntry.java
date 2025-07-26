package de.hetzge.eclipse.aicoder.context;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.swt.graphics.Image;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.AiCoderImageKey;
import de.hetzge.eclipse.aicoder.util.ContextUtils;

public class DependenciesContextEntry extends ContextEntry {

	public static final String PREFIX = "DEPENDENCIES";

	private DependenciesContextEntry(List<DependencyContextEntry> entries, Duration creationDuration) {
		super(entries, creationDuration);
	}

	@Override
	public ContextEntryKey getKey() {
		return new ContextEntryKey(PREFIX, PREFIX);
	}

	@Override
	public String getLabel() {
		return "Dependencies";
	}

	@Override
	public Image getImage() {
		return AiCoderActivator.getImage(AiCoderImageKey.DEPENDENCIES_ICON);
	}

	@Override
	public String getContent(ContextContext context) {
		return ContextUtils.contentTemplate("Dependencies", super.getContent(context));
	}

	public static DependenciesContextEntry create(IProject project) throws JavaModelException {
		final long before = System.currentTimeMillis();
		final List<DependencyContextEntry> entries = new ArrayList<>();
		final IJavaProject javaProject = JavaCore.create(project);
		if (javaProject != null && javaProject.exists()) {
			for (final IClasspathEntry classpathEntry : javaProject.getReferencedClasspathEntries()) {
				if (classpathEntry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
					entries.add(DependencyContextEntry.create(classpathEntry.getPath().toOSString()));
				}
			}
		}
		return new DependenciesContextEntry(entries, Duration.ofMillis(System.currentTimeMillis() - before));
	}
}

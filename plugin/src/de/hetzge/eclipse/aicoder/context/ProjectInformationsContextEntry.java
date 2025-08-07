package de.hetzge.eclipse.aicoder.context;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.swt.graphics.Image;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.AiCoderImageKey;
import de.hetzge.eclipse.aicoder.util.ContextUtils;

public class ProjectInformationsContextEntry extends ContextEntry {

	public static final String PREFIX = "PROJECT_INFORMATIONS";

	private ProjectInformationsContextEntry(List<ProjectInformationContextEntry> entries, Duration creationDuration) {
		super(entries, creationDuration);
	}

	@Override
	public ContextEntryKey getKey() {
		return new ContextEntryKey(PREFIX, PREFIX);
	}

	@Override
	public String getLabel() {
		return "Project informations";
	}

	@Override
	public Image getImage() {
		return AiCoderActivator.getImage(AiCoderImageKey.INFORMATIONS_ICON);
	}

	@Override
	public String getContent(ContextContext context) {
		return ContextUtils.contentTemplate("General project informations", super.getContent(context));
	}

	public static ContextEntryFactory factory(IProject project) {
		return new ContextEntryFactory(PREFIX, () -> create(project));
	}

	public static ContextEntry create(IProject project) {
		final long before = System.currentTimeMillis();
		final List<ProjectInformationContextEntry> entries = new ArrayList<>();
		if (project != null) {
			entries.add(ProjectInformationContextEntry.create("Name", project.getName()));
			entries.add(ProjectInformationContextEntry.create("Path", project.getLocation().toString()));
			final IJavaProject javaProject = JavaCore.create(project);
			if (javaProject != null && javaProject.exists()) {
				entries.add(ProjectInformationContextEntry.create("Java Version", javaProject.getOption(JavaCore.COMPILER_COMPLIANCE, true)));
			}
		}
		return new ProjectInformationsContextEntry(entries, Duration.ofMillis(System.currentTimeMillis() - before));
	}
}

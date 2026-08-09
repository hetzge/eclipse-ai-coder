package de.hetzge.eclipse.aicoder.preferences;

import de.hetzge.eclipse.aicoder.context.ProjectInformationsContextEntry;

public class ProjectInformationsContextPreferencePage extends ContextTypePreferencePage {

	public static final String ID = "de.hetzge.eclipse.aicoder.preferences.context.project_informations";
	public static final String CONTEXT_PREFIX = ProjectInformationsContextEntry.PREFIX;

	public ProjectInformationsContextPreferencePage() {
		super(ProjectInformationsContextEntry.PREFIX);
	}
}

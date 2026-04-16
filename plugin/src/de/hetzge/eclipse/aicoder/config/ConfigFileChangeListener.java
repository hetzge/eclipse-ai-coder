package de.hetzge.eclipse.aicoder.config;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;

import de.hetzge.eclipse.aicoder.AiCoderActivator;

public class ConfigFileChangeListener implements IResourceChangeListener {

	private static final String CONFIG_FILE = "config.json";
	private final ConfigManager configManager;

	public ConfigFileChangeListener(ConfigManager configManager) {
		this.configManager = configManager;
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		// POST_CHANGE is the safest type — fires after the change is committed
		if (event.getType() != IResourceChangeEvent.POST_CHANGE) {
			return;
		}

		try {
			event.getDelta().accept(new IResourceDeltaVisitor() {
				@Override
				public boolean visit(IResourceDelta delta) throws CoreException {
					// Only care about config.json at project root
					if (delta.getResource().getType() == IResource.FILE
							&& delta.getResource().getName().equals(CONFIG_FILE)
							&& delta.getResource().getParent().getType() == IResource.PROJECT) {

						final int kind = delta.getKind();
						if ((kind & (IResourceDelta.CHANGED | IResourceDelta.ADDED)) != 0) {
							// Content actually changed?
							if ((delta.getFlags() & IResourceDelta.CONTENT) != 0
									|| (kind & IResourceDelta.ADDED) != 0) {
								final IProject project = (IProject) delta.getResource().getParent();
								ConfigFileChangeListener.this.configManager.reloadConfig(project);
							}
						} else if ((kind & IResourceDelta.REMOVED) != 0) {
							final IProject project = (IProject) delta.getResource().getParent();
							ConfigFileChangeListener.this.configManager.clearConfig(project);
						}

						return false; // no need to visit children of a file
					}
					return true; // keep visiting deeper
				}
			});
		} catch (final CoreException exception) {
			AiCoderActivator.log().error("Failed to process resource change event", exception);
		}
	}
}
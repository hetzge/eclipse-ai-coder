package de.hetzge.eclipse.aicoder.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

import de.hetzge.eclipse.aicoder.AiCoderActivator;

public class ConfigManager {

	private final Map<IProject, RootConfig> configs;
	private final ConfigFileChangeListener listener;

	public ConfigManager() {
		this.configs = new HashMap<>();
		this.listener = new ConfigFileChangeListener(this);
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this.listener, IResourceChangeEvent.POST_CHANGE);
	}

	/** Called on first access for a project */
	public Optional<RootConfig> getConfig(IProject project) {
		return Optional.ofNullable(this.configs.computeIfAbsent(project, it -> {
			try {
				final Optional<RootConfig> configOptional = load(it);
				if (configOptional.isEmpty()) {
					AiCoderActivator.log().info("No config file found for project: " + it.getName());
					return null;
				}
				return configOptional.get();
			} catch (final CoreException exception) {
				throw new RuntimeException("Failed to load config for project: " + it.getName(), exception);
			}
		}));
	}

	/** Called by the listener when config.json changes */
	void reloadConfig(IProject project) {
		try {
			final Optional<RootConfig> configOptional = load(project);
			if (configOptional.isEmpty()) {
				AiCoderActivator.log().info("No config file found for project: " + project.getName());
				return;
			}
			this.configs.put(project, configOptional.get());
		} catch (final CoreException exception) {
			throw new RuntimeException("Failed to load config for project: " + project.getName(), exception);
		}
	}

	/** Called by the listener when config.json is deleted */
	void clearConfig(IProject project) {
		this.configs.remove(project);
	}

	/** Call on plugin stop / dispose */
	public void dispose() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this.listener);
	}

	private Optional<RootConfig> load(IProject project) throws CoreException {
		final IFile file = project.getFile("aicoder.toml");
		if (!file.exists()) {
			return Optional.empty();
		}
		return Optional.of(RootConfig.parse(file.readString()));
	}
}
package de.hetzge.eclipse.aicoder;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class AiCoderActivator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "codestral-eclipse"; //$NON-NLS-1$

	// The shared instance
	private static AiCoderActivator plugin;

	/**
	 * The constructor
	 */
	public AiCoderActivator() {
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static AiCoderActivator getDefault() {
		return plugin;
	}

}

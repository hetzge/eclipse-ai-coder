package de.hetzge.eclipse.aicoder;

import java.net.MalformedURLException;
import java.net.URI;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class AiCoderActivator extends AbstractUIPlugin {

	public static final String PLUGIN_ID = "codestral-eclipse"; //$NON-NLS-1$

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

	public static AiCoderActivator getDefault() {
		return plugin;
	}

	@Override
	protected void initializeImageRegistry(ImageRegistry registry) {
		super.initializeImageRegistry(registry);
		try {
			registerImage(registry, AiCoderImageKey.PACKAGE_ICON, ImageDescriptor.createFromURL(URI.create("platform:/plugin/org.eclipse.jdt.ui/icons/full/obj16/package_obj.png").toURL()));
			registerImage(registry, AiCoderImageKey.TYPE_ICON, ImageDescriptor.createFromURL(URI.create("platform:/plugin/org.eclipse.jdt.ui/icons/full/elcl16/class_obj.png").toURL()));
			registerImage(registry, AiCoderImageKey.RESOURCE_ICON, ImageDescriptor.createFromURL(URI.create("platform:/plugin/org.eclipse.ui.navigator.resources/icons/full/obj16/otherprojects_workingsets.png").toURL()));
			registerImage(registry, AiCoderImageKey.SCOPE_ICON, ImageDescriptor.createFromURL(URI.create("platform:/plugin/org.eclipse.jdt.ui/icons/full/elcl16/javaassist_co.png").toURL()));
			registerImage(registry, AiCoderImageKey.COPY_ICON, ImageDescriptor.createFromURL(URI.create("platform:/plugin/org.eclipse.ui/icons/full/etool16/copy_edit.png").toURL()));
			registerImage(registry, AiCoderImageKey.BEFORE_ICON, ImageDescriptor.createFromURL(URI.create("platform:/plugin/org.eclipse.jdt.ui/icons/full/etool16/shift_l_edit.png").toURL()));
			registerImage(registry, AiCoderImageKey.AFTER_ICON, ImageDescriptor.createFromURL(URI.create("platform:/plugin/org.eclipse.jdt.ui/icons/full/etool16/shift_r_edit.png").toURL()));
		} catch (final MalformedURLException exception) {
			throw new RuntimeException(exception);
		}
	}

	private void registerImage(ImageRegistry registry, AiCoderImageKey imageKey, ImageDescriptor descriptor) {
		registry.put(imageKey.name(), descriptor);
	}

	public static Image getImage(AiCoderImageKey imageKey) {
		return AiCoderActivator.getDefault().getImageRegistry().get(imageKey.name());
	}

	public static ImageDescriptor getImageDescriptor(AiCoderImageKey imageKey) {
		return AiCoderActivator.getDefault().getImageRegistry().getDescriptor(imageKey.name());
	}

}

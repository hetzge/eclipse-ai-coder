package de.hetzge.eclipse.aicoder;

import java.net.MalformedURLException;
import java.net.URI;

import org.eclipse.core.runtime.ILog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class AiCoderActivator extends AbstractUIPlugin {

	public static final String PLUGIN_ID = "de.hetzge.eclipse.aicoder";

	private static AiCoderActivator plugin;

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
			registerImage(registry, AiCoderImageKey.ENUM_ICON, ImageDescriptor.createFromURL(URI.create("platform:/plugin/org.eclipse.jdt.ui/icons/full/obj16/enum_obj.png").toURL()));
			registerImage(registry, AiCoderImageKey.INTERFACE_ICON, ImageDescriptor.createFromURL(URI.create("platform:/plugin/org.eclipse.jdt.ui/icons/full/obj16/innerinterface_public_obj.png").toURL()));
			registerImage(registry, AiCoderImageKey.RECORD_ICON, ImageDescriptor.createFromURL(URI.create("platform:/plugin/org.eclipse.jdt.ui/icons/full/obj16/record_obj.png").toURL()));
			registerImage(registry, AiCoderImageKey.ANNOTATION_ICON, ImageDescriptor.createFromURL(URI.create("platform:/plugin/org.eclipse.jdt.ui/icons/full/obj16/annotation_obj.png").toURL()));
			registerImage(registry, AiCoderImageKey.RESOURCE_ICON, ImageDescriptor.createFromURL(URI.create("platform:/plugin/org.eclipse.ui.navigator.resources/icons/full/obj16/otherprojects_workingsets.png").toURL()));
			registerImage(registry, AiCoderImageKey.SCOPE_ICON, ImageDescriptor.createFromURL(URI.create("platform:/plugin/org.eclipse.jdt.ui/icons/full/elcl16/javaassist_co.png").toURL()));
			registerImage(registry, AiCoderImageKey.COPY_ICON, ImageDescriptor.createFromURL(URI.create("platform:/plugin/org.eclipse.ui/icons/full/etool16/copy_edit.png").toURL()));
			registerImage(registry, AiCoderImageKey.BEFORE_ICON, ImageDescriptor.createFromURL(URI.create("platform:/plugin/org.eclipse.jdt.ui/icons/full/etool16/shift_l_edit.png").toURL()));
			registerImage(registry, AiCoderImageKey.AFTER_ICON, ImageDescriptor.createFromURL(URI.create("platform:/plugin/org.eclipse.jdt.ui/icons/full/etool16/shift_r_edit.png").toURL()));
			registerImage(registry, AiCoderImageKey.PIN_ICON, ImageDescriptor.createFromURL(URI.create("platform:/plugin/org.eclipse.ui/icons/full/ovr16/pinned_ovr@2x.png").toURL()));
			registerImage(registry, AiCoderImageKey.IMPORT_ICON, ImageDescriptor.createFromURL(URI.create("platform:/plugin/org.eclipse.ui/icons/full/etool16/import_wiz.png").toURL()));
			registerImage(registry, AiCoderImageKey.BLACKLIST_ICON, ImageDescriptor.createFromURL(URI.create("platform:/plugin/org.eclipse.ui/icons/full/elcl16/trash.png").toURL()));
			registerImage(registry, AiCoderImageKey.FILL_IN_MIDDLE_ICON, ImageDescriptor.createFromURL(URI.create("platform:/plugin/org.eclipse.ui.workbench.texteditor/icons/full/elcl16/insert_template.png").toURL()));
			registerImage(registry, AiCoderImageKey.INFORMATIONS_ICON, ImageDescriptor.createFromURL(URI.create("platform:/plugin/org.eclipse.ui/icons/full/obj16/info_tsk.png").toURL()));
			registerImage(registry, AiCoderImageKey.DEPENDENCIES_ICON, ImageDescriptor.createFromURL(URI.create("platform:/plugin/org.eclipse.jdt.ui/icons/full/obj16/library_obj.png").toURL()));
			registerImage(registry, AiCoderImageKey.EDITOR_ICON, ImageDescriptor.createFromURL(URI.create("platform:/plugin/org.eclipse.ui/icons/full/etool16/editor_area.png").toURL()));
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

	public static ILog log() {
		return getDefault().getLog();
	}

}

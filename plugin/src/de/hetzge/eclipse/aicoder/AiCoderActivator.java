package de.hetzge.eclipse.aicoder;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import de.hetzge.eclipse.aicoder.config.ConfigManager;
import de.hetzge.eclipse.aicoder.content.InstructionStorage;
import de.hetzge.eclipse.aicoder.mcp.McpClients;

public class AiCoderActivator extends AbstractUIPlugin {

	public static final String PLUGIN_ID = "de.hetzge.eclipse.aicoder";

	private static AiCoderActivator plugin;
	private InstructionStorage instructionStorage;
	private EditorViewMemory editorViewMemory;
	private ConfigManager configManager;

	public AiCoderActivator() {
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		this.instructionStorage = InstructionStorage.load(getStateLocation());
		this.editorViewMemory = new EditorViewMemory(1000);
		this.configManager = new ConfigManager();
		McpClients.INSTANCE.reload(() -> {
			log().info("MCP clients loaded: " + McpClients.INSTANCE.getMcpStatusCountsString());
		});
		final IWorkbench workbench = PlatformUI.getWorkbench();
		final IWorkbenchWindow[] windows = workbench.getWorkbenchWindows();
		for (final IWorkbenchWindow window : windows) {
			final IWorkbenchPage page = window.getActivePage();
			if (page != null) {
				page.addPartListener(new AiCoderPartListener());
			}
		}
		workbench.addWindowListener(new AiCoderWindowListener());
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		if (this.configManager != null) {
			this.configManager.dispose();
		}
		plugin = null;
		super.stop(context);
	}

	public InstructionStorage getInstructionStorage() {
		return this.instructionStorage;
	}

	public EditorViewMemory getEditorViewMemory() {
		return this.editorViewMemory;
	}

	public ConfigManager getConfigManager() {
		return this.configManager;
	}

	public static AiCoderActivator getDefault() {
		return plugin;
	}

	@Override
	protected void initializeImageRegistry(ImageRegistry registry) {
		super.initializeImageRegistry(registry);
		AiCoderImageKey.initializeImages(registry);
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

	public static void openErrorDialog(String title, String message, Throwable throwable) {
		ErrorDialog.openError(null, title, null, new Status(IStatus.ERROR, AiCoderActivator.PLUGIN_ID, message, throwable));
	}

}

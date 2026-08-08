package de.hetzge.eclipse.aicoder;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
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

import de.hetzge.eclipse.aicoder.agent.AgentService;
import de.hetzge.eclipse.aicoder.agent.AgentTasksState;
import de.hetzge.eclipse.aicoder.config.ConfigManager;
import de.hetzge.eclipse.aicoder.content.InstructionStorage;
import de.hetzge.eclipse.aicoder.mcp.McpClients;

// TODO
// - on switch editor abort inline completions/suggestions
// - on close eclipse window abort suggestions (?!)
// - fix added lines in suggestions (popup offsets)
// - use AGENT context for agents
// - abort agent action
// - agent task file open diff action (original change)
// - agent task file open compare action (compare with working tree)
// - agent task apply all
// - agent task revert to reference state
// - if suggestion apply makes the editor blank then ask if file should be deleted
// - tools selection in dialog
// - additional tools: compile project, get warnings of file/folder

public class AiCoderActivator extends AbstractUIPlugin {

	public static final String PLUGIN_ID = "de.hetzge.eclipse.aicoder";

	private static AiCoderActivator plugin;
	private InstructionStorage instructionStorage;
	private EditorViewMemory editorViewMemory;
	private ConfigManager configManager;
	private AgentTasksState agentTasksState;
	private AgentService agentService;

	public AiCoderActivator() {
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		this.instructionStorage = InstructionStorage.load(getStateLocation());
		this.editorViewMemory = new EditorViewMemory(1000);
		this.configManager = new ConfigManager();
		this.agentTasksState = new AgentTasksState();
		final Job loadAgentTasksJob = new Job("Load agent tasks") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					AiCoderActivator.getDefault().getAgentTasksState().load();
					return Status.OK_STATUS;
				} catch (final Exception exception) {
					return new Status(IStatus.ERROR, PLUGIN_ID, "Failed to load agent tasks", exception);
				}
			}
		};
		loadAgentTasksJob.schedule();
		loadAgentTasksJob.join(); // TODO
		this.agentService = new AgentService();
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

	public AgentTasksState getAgentTasksState() {
		return this.agentTasksState;
	}

	public AgentService getAgentService() {
		return this.agentService;
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

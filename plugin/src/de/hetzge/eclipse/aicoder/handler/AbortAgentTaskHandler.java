package de.hetzge.eclipse.aicoder.handler;

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.agent.AgentService;
import de.hetzge.eclipse.aicoder.agent.AgentStatus;
import de.hetzge.eclipse.aicoder.agent.AgentTask;
import de.hetzge.eclipse.aicoder.agent.AgentTaskTreeView;

public final class AbortAgentTaskHandler extends AbstractHandler {

	public static final String COMMAND_ID = "de.hetzge.eclipse.codestral.commands.abortAgentTask";

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final AgentTaskTreeView agentTaskTreeView = resolveTreeView(event);
		if (agentTaskTreeView == null) {
			AiCoderActivator.log().warn("Agent task tree view not found, nothing to abort");
			return null;
		}
		final Set<AgentTask> tasks = new LinkedHashSet<>(agentTaskTreeView.getSelectedAgentTasks());
		final AgentService agentService = AiCoderActivator.getDefault().getAgentService();
		tasks.stream()
				.filter(task -> task.getStatus() == AgentStatus.RUNNING)
				.map(AgentTask::getId)
				.forEach(agentService::abort);
		return null;
	}

	private AgentTaskTreeView resolveTreeView(ExecutionEvent event) {
		final IWorkbenchPart activePart = HandlerUtil.getActivePart(event);
		if (activePart instanceof final AgentTaskTreeView agentTaskTreeView) {
			return agentTaskTreeView;
		}
		final IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window != null && window.getActivePage() != null) {
			return (AgentTaskTreeView) window.getActivePage().findView(AgentTaskTreeView.ID);
		}
		return null;
	}
}

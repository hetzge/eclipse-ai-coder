package de.hetzge.eclipse.aicoder.handler;

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.agent.AgentService;
import de.hetzge.eclipse.aicoder.agent.AgentTask;
import de.hetzge.eclipse.aicoder.agent.AgentTaskTreeView;

public final class RerunAgentTaskHandler extends AbstractHandler {

	public static final String COMMAND_ID = "de.hetzge.eclipse.codestral.commands.rerunAgentTask";

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final AgentTaskTreeView agentTaskTreeView = AgentTaskTreeView.resolveTreeView(event);
		if (agentTaskTreeView == null) {
			AiCoderActivator.log().warn("Agent task tree view not found, nothing to rerun");
			return null;
		}

		final List<AgentTask> tasks = agentTaskTreeView.getSelectedAgentTasks();
		final AgentService agentService = AiCoderActivator.getDefault().getAgentService();

		for (final AgentTask task : tasks) {
			agentService.rerun(task.getId());
		}

		return null;
	}

}
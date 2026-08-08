package de.hetzge.eclipse.aicoder.handler;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import de.hetzge.eclipse.aicoder.AiCoderActivator;

public final class AbortAllAgentTasksHandler extends AbstractHandler {

	public static final String COMMAND_ID = "de.hetzge.eclipse.codestral.commands.abortAllAgentTasks";

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		AiCoderActivator.getDefault().getAgentService().abortAll();
		return null;
	}
}

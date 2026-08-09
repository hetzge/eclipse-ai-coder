package de.hetzge.eclipse.aicoder.agent;

import java.util.UUID;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;

import de.hetzge.eclipse.aicoder.AiCoderActivator;

public class AgentTrajectoryEditorInputFactory implements IElementFactory {
	public static final String ID = "de.hetzge.eclipse.aicoder.AgentTrajectoryInputFactory";

	@Override
	public IAdaptable createElement(IMemento memento) {
		final String id = memento.getString("agentTaskId");
		final AgentTask task = AiCoderActivator.getDefault().getAgentTasksState().findTask(UUID.fromString(id)).orElse(null);
		return task != null ? new AgentTrajectoryEditorInput(task) : null;
	}
}
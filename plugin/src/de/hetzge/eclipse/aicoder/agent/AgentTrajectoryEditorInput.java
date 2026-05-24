package de.hetzge.eclipse.aicoder.agent;

import java.util.Objects;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.AiCoderImageKey;

public final class AgentTrajectoryEditorInput implements IEditorInput {

	private final AgentTask agentTask;

	public AgentTrajectoryEditorInput(AgentTask agentTask) {
		this.agentTask = agentTask;
	}

	public AgentTask getAgentTask() {
		return this.agentTask;
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		return null;
	}

	@Override
	public boolean exists() {
		return true;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return AiCoderActivator.getImageDescriptor(AiCoderImageKey.FILL_IN_MIDDLE_ICON); // TODO
	}

	@Override
	public String getName() {
		return this.agentTask.getTitle();
	}

	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	@Override
	public String getToolTipText() {
		return this.agentTask.getTitle();
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.agentTask);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final AgentTrajectoryEditorInput other = (AgentTrajectoryEditorInput) obj;
		return Objects.equals(this.agentTask, other.agentTask);
	}
}
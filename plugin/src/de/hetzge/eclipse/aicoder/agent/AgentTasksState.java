package de.hetzge.eclipse.aicoder.agent;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.runtime.ListenerList;

import de.hetzge.eclipse.aicoder.llm.LlmMessage;

public final class AgentTasksState {

	private List<AgentTask> agentTasks;
	private final ListenerList<AgentTasksStateListener> listeners;
	private final ListenerList<AgentTrajectoryStateListener> trajectoryListeners;

	public AgentTasksState() {
		this.agentTasks = List.of();
		this.listeners = new ListenerList<>();
		this.trajectoryListeners = new ListenerList<>();
	}

	public List<AgentTask> getAgentTasks() {
		return this.agentTasks;
	}

	public synchronized List<AgentTask> updateAndGetAgentTasks() throws IOException {
		this.agentTasks = AgentStorage.loadAgentTasks();
		return this.agentTasks;
	}

	public synchronized void appendTrajectory(UUID id, LlmMessage message) throws IOException {
		AgentStorage.appendTrajectory(id, message);
		fireAgentTrajectoryChanged(id, message);
	}

	public synchronized void addListener(AgentTasksStateListener listener) {
		this.listeners.add(listener);
	}

	public synchronized void removeListener(AgentTasksStateListener listener) {
		this.listeners.remove(listener);
	}

	private void fireAgentTasksChanged() {
		for (final AgentTasksStateListener listener : this.listeners) {
			listener.onAgentTasksChanged();
		}
	}

	public synchronized void addTrajectoryListener(UUID agentTaskId, AgentTrajectoryStateListener listener) throws IOException {
		final List<LlmMessage> trajectory = AgentStorage.loadTrajectory(agentTaskId);
		for (final LlmMessage message : trajectory) {
			listener.onAgentTrajectoryChanged(agentTaskId, message);
		}
		this.trajectoryListeners.add((id, message) -> {
			if (id.equals(agentTaskId)) {
				listener.onAgentTrajectoryChanged(id, message);
			}
		});
	}

	public synchronized void removeTrajectoryListener(AgentTrajectoryStateListener listener) {
		this.trajectoryListeners.remove(listener);
	}

	private void fireAgentTrajectoryChanged(UUID id, LlmMessage message) {
		for (final AgentTrajectoryStateListener listener : this.trajectoryListeners) {
			listener.onAgentTrajectoryChanged(id, message);
		}
	}

	@FunctionalInterface
	public static interface AgentTasksStateListener {
		void onAgentTasksChanged();
	}

	@FunctionalInterface
	public static interface AgentTrajectoryStateListener {
		void onAgentTrajectoryChanged(UUID agentTaskId, LlmMessage message);
	}
}

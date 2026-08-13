package de.hetzge.eclipse.aicoder.agent;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import org.eclipse.core.runtime.ListenerList;

import com.github.f4b6a3.uuid.util.UuidComparator;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.trajectory.TrajectoryEntry;

public final class AgentTasksState {

	private final Set<AgentTask> agentTasks;
	private final ListenerList<AgentTasksStateListener> listeners;
	private final ListenerList<AgentTrajectoryStateListener> trajectoryListeners;

	public AgentTasksState() {
		this.agentTasks = new TreeSet<>(Comparator.comparing(AgentTask::getId, UuidComparator.getDefaultInstance().reversed()));
		this.listeners = new ListenerList<>();
		this.trajectoryListeners = new ListenerList<>();
	}

	public synchronized void load() throws IOException {
		this.agentTasks.clear();
		this.agentTasks.addAll(AgentStorage.loadAgentTasks());
		AiCoderActivator.log().info("Loaded " + this.agentTasks.size() + " agent tasks");
		for (final AgentTask agentTask : this.agentTasks) {
			fireAgentTasksChanged(agentTask);
		}
	}

	public synchronized Optional<AgentTask> findTask(UUID id) {
		return this.agentTasks.stream().filter(task -> task.getId().equals(id)).findFirst();
	}

	public synchronized void saveAgentTask(AgentTask task) throws IOException {
		AgentStorage.saveAgentTask(task);
		this.agentTasks.add(task);
		fireAgentTasksChanged(task);
	}

	public synchronized List<AgentTask> getAgentTasks() {
		return List.copyOf(this.agentTasks);
	}

	public synchronized void deleteAgentTask(UUID id) throws IOException {
		final Optional<AgentTask> taskOptional = findTask(id);
		AgentStorage.deleteAgentTask(id);
		if (taskOptional.isPresent()) {
			final AgentTask task = taskOptional.get();
			this.agentTasks.remove(task);
			fireAgentTasksChanged(task);
		}
	}

	public synchronized void appendTrajectory(UUID id, TrajectoryEntry entry) throws IOException {
		AgentStorage.appendTrajectory(id, entry);
		fireAgentTrajectoryChanged(id, entry);
	}

	public synchronized void addListener(AgentTasksStateListener listener) {
		this.listeners.add(listener);
	}

	public synchronized void removeListener(AgentTasksStateListener listener) {
		this.listeners.remove(listener);
	}

	public synchronized void fireAgentTasksChanged(AgentTask task) {
		for (final AgentTasksStateListener listener : this.listeners) {
			listener.onAgentTasksChanged(task);
		}
	}

	public synchronized Optional<String> loadAgentResultMessage(UUID id) throws IOException {
		return AgentStorage.loadLastAssistantMessageContent(id);
	}

	public synchronized void loadAndAddTrajectoryListener(UUID agentTaskId, AgentTrajectoryStateListener listener) throws IOException {
		final List<TrajectoryEntry> entries = AgentStorage.loadTrajectory(agentTaskId);
		for (final TrajectoryEntry entry : entries) {
			listener.onAgentTrajectoryChanged(agentTaskId, entry);
		}
		addTrajectoryListener(agentTaskId, listener);
	}

	public synchronized void addTrajectoryListener(UUID agentTaskId, AgentTrajectoryStateListener listener) {
		this.trajectoryListeners.add((id, entry) -> {
			if (id.equals(agentTaskId)) {
				listener.onAgentTrajectoryChanged(id, entry);
			}
		});
	}

	public synchronized void removeTrajectoryListener(AgentTrajectoryStateListener listener) {
		this.trajectoryListeners.remove(listener);
	}

	private void fireAgentTrajectoryChanged(UUID id, TrajectoryEntry entry) {
		for (final AgentTrajectoryStateListener listener : this.trajectoryListeners) {
			listener.onAgentTrajectoryChanged(id, entry);
		}
	}

	@FunctionalInterface
	public static interface AgentTasksStateListener {
		void onAgentTasksChanged(AgentTask task);
	}

	@FunctionalInterface
	public static interface AgentTrajectoryStateListener {
		void onAgentTrajectoryChanged(UUID agentTaskId, TrajectoryEntry entry);
	}
}

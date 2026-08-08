package de.hetzge.eclipse.aicoder.agent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.eclipse.core.resources.IWorkspace;

import com.github.f4b6a3.uuid.util.UuidUtil;

import mjson.Json;

public final class AgentTask {

	private final UUID id;
	private final String title;
	private final AgentRequest request;
	private AgentStatus status;
	private final List<AgentChange> changes;

	public AgentTask(UUID id, String title, AgentRequest request) {
		this(id, title, request, new ArrayList<>());
	}

	public AgentTask(UUID id, String title, AgentRequest request, List<AgentChange> changes) {
		this.id = id;
		this.title = title;
		this.request = request;
		this.status = AgentStatus.RUNNING;
		this.changes = changes;
	}

	public UUID getId() {
		return this.id;
	}

	public Instant getCreationTime() {
		return UuidUtil.getInstant(this.id);
	}

	public String getTitle() {
		return this.title;
	}

	public AgentRequest getRequest() {
		return this.request;
	}

	public AgentStatus getStatus() {
		return this.status;
	}

	public void setStatus(AgentStatus status) {
		this.status = status;
	}

	public List<AgentChange> getChanges() {
		return this.changes;
	}

	public void setChanges(List<AgentChange> changes) {
		this.changes.clear();
		this.changes.addAll(changes);
	}

	public Json toJson() {
		final Json json = Json.object();
		json.set("id", this.id.toString());
		json.set("title", this.title);
		json.set("request", this.request.toJson());
		json.set("status", this.status.name());
		json.set("changes", this.changes.stream().map(AgentChange::toJson).toList());
		return json;
	}

	public static AgentTask fromJson(IWorkspace workspace, Json json) {
		final AgentTask agentTask = new AgentTask(
				UUID.fromString(json.at("id").asString()),
				json.at("title").asString(),
				AgentRequest.fromJson(workspace, json.at("request")),
				json.at("changes", Json.array()).asJsonList().stream().map(AgentChange::fromJson).toList());
		agentTask.setStatus(AgentStatus.valueOf(json.at("status").asString()));
		return agentTask;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.id);
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
		final AgentTask other = (AgentTask) obj;
		return Objects.equals(this.id, other.id);
	}
}

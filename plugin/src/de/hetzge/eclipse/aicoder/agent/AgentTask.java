package de.hetzge.eclipse.aicoder.agent;

import java.util.UUID;

import org.eclipse.core.resources.IWorkspace;

import de.hetzge.eclipse.aicoder.agent.AgentRequest.ProjectNotFoundException;
import mjson.Json;

public record AgentTask(UUID id, String title, AgentRequest request) {

	public Json toJson() {
		return Json.object()
				.set("id", this.id.toString())
				.set("title", this.title)
				.set("request", this.request.toJson());
	}

	public static AgentTask fromJson(IWorkspace workspace, Json json) throws ProjectNotFoundException {
		return new AgentTask(
				UUID.fromString(json.at("id").asString()),
				json.at("title").asString(),
				AgentRequest.fromJson(workspace, json.at("request")));
	}
}

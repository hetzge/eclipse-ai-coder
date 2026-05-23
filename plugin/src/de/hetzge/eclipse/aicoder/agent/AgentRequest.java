package de.hetzge.eclipse.aicoder.agent;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;

import de.hetzge.eclipse.aicoder.llm.LlmOption;
import mjson.Json;

public record AgentRequest(IProject project, LlmOption llmOption, String instructions) {

	public Json toJson() {
		return Json.object()
				.set("project", this.project.getName())
				.set("llmOption", this.llmOption.toJson())
				.set("instructions", this.instructions);
	}

	public static AgentRequest fromJson(IWorkspace workspace, Json json) throws ProjectNotFoundException {
		final IProject project = workspace.getRoot().getProject(json.at("project").asString());
		if (!project.exists()) {
			throw new ProjectNotFoundException("Project not found: " + json.at("project").asString());
		}
		return new AgentRequest(
				project,
				LlmOption.fromJson(json.at("llmOption")),
				json.at("instructions").asString());
	}

	public static class ProjectNotFoundException extends Exception {
		public ProjectNotFoundException(String message) {
			super(message);
		}
	}
}

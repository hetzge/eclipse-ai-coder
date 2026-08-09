package de.hetzge.eclipse.aicoder.agent;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;

import de.hetzge.eclipse.aicoder.base.TextSelection;
import de.hetzge.eclipse.aicoder.llm.LlmOption;
import mjson.Json;

public record AgentRequest(List<IProject> projects, LlmOption llmOption, TextSelection selection, String instructions, boolean readonly) {

	public Json toJson() {
		return Json.object()
				.set("projects", this.projects.stream().map(IProject::getName).toList())
				.set("llmOption", this.llmOption.toJson())
				.set("selection", this.selection.toJson())
				.set("instructions", this.instructions)
				.set("readonly", this.readonly);
	}

	public static AgentRequest fromJson(IWorkspace workspace, Json json) {
		return new AgentRequest(
				json.at("projects").asJsonList().stream()
						.map(Json::asString)
						.map(workspace.getRoot()::getProject)
						.filter(IProject::exists)
						.toList(),
				LlmOption.fromJson(json.at("llmOption")),
				TextSelection.fromJson(json.at("selection")),
				json.at("instructions").asString(),
				json.at("readonly", false).asBoolean());
	}
}

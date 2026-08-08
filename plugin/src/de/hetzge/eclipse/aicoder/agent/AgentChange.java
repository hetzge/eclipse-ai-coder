package de.hetzge.eclipse.aicoder.agent;

import org.eclipse.core.runtime.IPath;

import mjson.Json;

public record AgentChange(IPath path, AgentChangeType type, int linesAdded, int linesRemoved) {
	public Json toJson() {
		final Json json = Json.object();
		json.set("path", this.path.toPortableString());
		json.set("type", this.type.name());
		json.set("linesAdded", this.linesAdded);
		json.set("linesRemoved", this.linesRemoved);
		return json;
	}

	public static AgentChange fromJson(Json json) {
		return new AgentChange(
				IPath.fromPortableString(json.at("path").asString()),
				AgentChangeType.valueOf(json.at("type").asString()),
				json.at("linesAdded", 0).asInteger(),
				json.at("linesRemoved", 0).asInteger());
	}
}

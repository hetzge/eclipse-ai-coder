package de.hetzge.eclipse.aicoder.tool;

import org.eclipse.core.runtime.IProgressMonitor;

import mjson.Json;

public abstract class Tool {

	private final Json definition;

	public Tool(Json definition) {
		this.definition = definition;
	}

	public Json getDefinition() {
		return this.definition;
	}

	public String getName() {
		return this.definition.at("name").asString();
	}

	public abstract String execute(IProgressMonitor monitor, Json arguments);

}

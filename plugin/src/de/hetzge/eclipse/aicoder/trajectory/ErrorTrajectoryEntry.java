package de.hetzge.eclipse.aicoder.trajectory;

import java.util.Optional;

import mjson.Json;

public record ErrorTrajectoryEntry(String message) implements TrajectoryEntry {

	public static final String TYPE = "ERROR";

	@Override
	public Json toJson() {
		return Json.object()
				.set("type", TYPE)
				.set("message", this.message);
	}

	public static Optional<ErrorTrajectoryEntry> fromJson(Json json) {
		if (!TYPE.equals(json.at("type").asString())) {
			return Optional.empty();
		}
		return Optional.of(new ErrorTrajectoryEntry(json.at("message").asString()));
	}
}

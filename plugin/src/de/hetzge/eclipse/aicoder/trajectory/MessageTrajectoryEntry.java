package de.hetzge.eclipse.aicoder.trajectory;

import java.util.Optional;

import de.hetzge.eclipse.aicoder.llm.LlmMessage;
import mjson.Json;

public record MessageTrajectoryEntry(LlmMessage message) implements TrajectoryEntry {

	public static final String TYPE = "MESSAGE";

	@Override
	public Json toJson() {
		return Json.object()
				.set("type", TYPE)
				.set("message", this.message.toJson());
	}

	public static Optional<MessageTrajectoryEntry> fromJson(Json json) {
		if (!TYPE.equals(json.at("type").asString())) {
			return Optional.empty();
		}
		return Optional.of(new MessageTrajectoryEntry(LlmMessage.fromJson(json.at("message"))));
	}
}

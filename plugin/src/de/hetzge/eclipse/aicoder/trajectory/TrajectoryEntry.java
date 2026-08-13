package de.hetzge.eclipse.aicoder.trajectory;

import java.util.List;
import java.util.Optional;

import mjson.Json;

public interface TrajectoryEntry {
	Json toJson();

	static Optional<TrajectoryEntry> fromJson(Json json) {
		return List.of(
				MessageTrajectoryEntry.fromJson(json),
				ErrorTrajectoryEntry.fromJson(json))
				.stream()
				.filter(Optional::isPresent)
				.map(it -> it.map(entry -> (TrajectoryEntry) entry).get())
				.findFirst();
	}
}

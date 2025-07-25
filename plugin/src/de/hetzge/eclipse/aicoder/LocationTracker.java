package de.hetzge.eclipse.aicoder;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public final class LocationTracker {

	private final LinkedList<CodeLocation> locations;
	private final int maxEntries;
	private final int tolerance;

	public LocationTracker(int maxEntries, int tolerance) {
		this.maxEntries = maxEntries;
		this.tolerance = tolerance;
		this.locations = new LinkedList<>();
	}

	public void addLocation(String key, int line) {
		this.addLocation(new CodeLocation(key, new CodeRange(line - this.tolerance, line + this.tolerance)));
	}

	public void addLocation(CodeLocation newLocation) {
		final List<CodeLocation> overlappingLocations = this.locations.stream()
				.filter(location -> Objects.equals(location.key(), newLocation.key()))
				.filter(location -> location.range().doesOverlap(newLocation.range()))
				.toList();
		this.locations.removeAll(overlappingLocations);
		final CodeLocation joinedLocation = overlappingLocations.stream()
				.reduce(newLocation, (acc, location) -> new CodeLocation(acc.key(), new CodeRange(
						Math.min(acc.range().firstLine(), location.range().firstLine()),
						Math.max(acc.range().lastLine(), location.range().lastLine()))));
		this.locations.add(joinedLocation);
		while (this.locations.size() > this.maxEntries) {
			this.locations.removeFirst();
		}
	}

	public List<CodeLocation> getLocations() {
		return List.copyOf(this.locations);
	}

	private static record CodeRange(int firstLine, int lastLine) {
		public boolean doesOverlap(CodeRange other) {
			return this.firstLine <= other.lastLine && this.lastLine >= other.firstLine;
		}
	}

	private static record CodeLocation(String key, CodeRange range) {
	}

}

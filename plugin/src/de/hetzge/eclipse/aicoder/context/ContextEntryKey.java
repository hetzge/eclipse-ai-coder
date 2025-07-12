package de.hetzge.eclipse.aicoder.context;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

public record ContextEntryKey(
		String prefix,
		String value) {

	public String getKeyString() {
		return String.format("%s::%s", this.prefix, Base64.getEncoder().encodeToString(this.value.getBytes(StandardCharsets.UTF_8)));
	}

	public static Optional<ContextEntryKey> parseKeyString(String keyString) {
		final String[] parts = keyString.split("::");
		if (parts.length != 2) {
			return Optional.empty();
		}
		return Optional.of(new ContextEntryKey(parts[0], new String(Base64.getDecoder().decode(parts[1]), StandardCharsets.UTF_8)));
	}
}
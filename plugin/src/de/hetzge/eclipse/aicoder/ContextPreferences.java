package de.hetzge.eclipse.aicoder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.Preferences;

import de.hetzge.eclipse.aicoder.context.ContextEntryKey;
import de.hetzge.eclipse.aicoder.context.CustomContextEntry;
import mjson.Json;

public final class ContextPreferences {
	private static final String BLACKLIST_PREFERENCE_KEY = "context_blacklist";
	private static final String STICKYLIST_PREFERENCE_KEY = "context_stickylist";
	private static final String CUSTOM_CONTEXT_PREFERENCE_KEY = "custom_context";
	private static final String CONTEXT_TYPE_POSITIONS_PREFERENCE_KEY = "context_type_positions";
	private static final String PREFERENCES_PREFERENCE_NODE = "de.hetzge.eclipse.aicoder";

	private static final Set<ContextEntryKey> BLACKLIST = new HashSet<>();
	private static final Set<ContextEntryKey> STICKYLIST = new HashSet<>();
	private static final List<CustomContextEntry> CUSTOM_CONTEXT_ENTRIES = new ArrayList<>();
	private static final List<ContextTypePositionItem> CONTEXT_TYPE_POSITIONS = new ArrayList<>();

	static {
		loadPreferences();
	}

	private ContextPreferences() {
	}

	private static void loadPreferences() {
		final Preferences preferences = InstanceScope.INSTANCE.getNode(PREFERENCES_PREFERENCE_NODE);

		// Load blacklist
		final String blacklistString = preferences.get(BLACKLIST_PREFERENCE_KEY, "");
		for (final String keyString : blacklistString.split(",")) {
			final Optional<ContextEntryKey> optional = ContextEntryKey.parseKeyString(keyString);
			if (optional.isPresent()) {
				BLACKLIST.add(optional.get());
			} else {
				AiCoderActivator.log().warn(String.format("Failed to read blacklist key: '%s'", keyString));
			}
		}

		// Load stickylist
		final String stickylistString = preferences.get(STICKYLIST_PREFERENCE_KEY, "");
		for (final String keyString : stickylistString.split(",")) {
			final Optional<ContextEntryKey> optional = ContextEntryKey.parseKeyString(keyString);
			if (optional.isPresent()) {
				STICKYLIST.add(optional.get());
			} else {
				AiCoderActivator.log().warn(String.format("Failed to read sticky key: '%s'", keyString));
			}
		}

		// Load user/custom context
		final String userContextString = preferences.get(CUSTOM_CONTEXT_PREFERENCE_KEY, "[]");
		CUSTOM_CONTEXT_ENTRIES.addAll(Json.read(userContextString).asJsonList().stream().map(CustomContextEntry::createFromJson).toList());

		// Load context type positions
		final String contextTypePositionsString = preferences.get(CONTEXT_TYPE_POSITIONS_PREFERENCE_KEY, "[]");
		CONTEXT_TYPE_POSITIONS.addAll(Json.read(contextTypePositionsString).asJsonList().stream().map(ContextTypePositionItem::createFromJson).toList());
	}

	private static void savePreferences() {
		final Preferences preferences = InstanceScope.INSTANCE.getNode(PREFERENCES_PREFERENCE_NODE);

		// Save blacklist
		final String blacklistString = BLACKLIST.stream()
				.map(ContextEntryKey::getKeyString)
				.collect(Collectors.joining(","));
		preferences.put(BLACKLIST_PREFERENCE_KEY, blacklistString);

		// Save stickylist
		final String stickylistString = STICKYLIST.stream()
				.map(ContextEntryKey::getKeyString)
				.collect(Collectors.joining(","));
		preferences.put(STICKYLIST_PREFERENCE_KEY, stickylistString);

		// Save user/custom context
		final String userContextString = Json.array(CUSTOM_CONTEXT_ENTRIES.stream()
				.map(CustomContextEntry::toJson)
				.toArray()).toString();
		preferences.put(CUSTOM_CONTEXT_PREFERENCE_KEY, userContextString);

		// Save context type positions
		final String contextTypePositionsString = Json.array(CONTEXT_TYPE_POSITIONS.stream()
				.map(ContextTypePositionItem::toJson)
				.toArray()).toString();
		preferences.put(CONTEXT_TYPE_POSITIONS_PREFERENCE_KEY, contextTypePositionsString);

		try {
			preferences.flush();
		} catch (final Exception exception) {
			AiCoderActivator.log().error("Failed to save preferences", exception);
		}
	}

	public static void addToBlacklist(ContextEntryKey entry) {
		BLACKLIST.add(entry);
		savePreferences();
	}

	public static void removeFromBlacklist(ContextEntryKey entry) {
		BLACKLIST.remove(entry);
		savePreferences();
	}

	public static boolean isBlacklisted(ContextEntryKey entry) {
		return BLACKLIST.contains(entry);
	}

	public static void addToStickylist(ContextEntryKey entry) {
		STICKYLIST.add(entry);
		savePreferences();
	}

	public static void removeFromStickylist(ContextEntryKey entry) {
		STICKYLIST.remove(entry);
		savePreferences();
	}

	public static boolean isSticky(ContextEntryKey entry) {
		return STICKYLIST.contains(entry);
	}

	public static Set<ContextEntryKey> getBlacklist() {
		return new HashSet<>(BLACKLIST);
	}

	public static Set<ContextEntryKey> getStickylist() {
		return new HashSet<>(STICKYLIST);
	}

	public static List<CustomContextEntry> getCustomContextEntries() {
		return CUSTOM_CONTEXT_ENTRIES;
	}

	public static void setCustomContextEntries(List<CustomContextEntry> newEntries) {
		CUSTOM_CONTEXT_ENTRIES.clear();
		CUSTOM_CONTEXT_ENTRIES.addAll(newEntries);
		savePreferences();
	}

	public static List<ContextTypePositionItem> getContextTypePositions() {
		return CONTEXT_TYPE_POSITIONS;
	}

	public static Map<String, ContextTypePositionItem> getContextTypePositionByPrefix() {
		return CONTEXT_TYPE_POSITIONS.stream().collect(Collectors.toMap(ContextTypePositionItem::prefix, Function.identity()));
	}

	public static void setContextTypePositions(List<ContextTypePositionItem> newPositions) {
		CONTEXT_TYPE_POSITIONS.clear();
		CONTEXT_TYPE_POSITIONS.addAll(newPositions);
		savePreferences();
	}

	public record ContextTypePositionItem(String prefix, boolean enabled, int position) {

		public ContextTypePositionItem withEnabled(boolean enabled) {
			return new ContextTypePositionItem(this.prefix, enabled, this.position);
		}

		public ContextTypePositionItem withPosition(int position) {
			return new ContextTypePositionItem(this.prefix, this.enabled, position);
		}

		public Json toJson() {
			return Json.object()
					.set("prefix", this.prefix)
					.set("enabled", this.enabled)
					.set("position", this.position);
		}

		public static ContextTypePositionItem createFromJson(Json json) {
			return new ContextTypePositionItem(
					json.at("prefix").asString(),
					json.at("enabled").asBoolean(),
					json.at("position").asInteger());
		}
	}
}
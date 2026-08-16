package de.hetzge.eclipse.aicoder.preferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.Preferences;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.CompletionMode;
import de.hetzge.eclipse.aicoder.context.ContextEntryKey;
import de.hetzge.eclipse.aicoder.context.CustomContextEntryData;
import de.hetzge.eclipse.aicoder.context.FillInMiddleContextEntry;
import mjson.Json;

public final class ContextPreferences {
	private static final String BLACKLIST_PREFERENCE_KEY = "context_blacklist";
	private static final String STICKYLIST_PREFERENCE_KEY = "context_stickylist";
	private static final String CUSTOM_CONTEXT_PREFERENCE_KEY = "custom_context";
	private static final String CONTEXT_TYPE_POSITIONS_PREFERENCE_KEY = "context_type_positions";
	private static final String PREFERENCES_PREFERENCE_NODE = "de.hetzge.eclipse.aicoder";

	private final static Map<CompletionMode, ContextPreferences> contextPreferencesByMode;

	static {
		contextPreferencesByMode = new HashMap<>();
		for (final CompletionMode mode : CompletionMode.values()) {
			final ContextPreferences preferences = new ContextPreferences(mode, PREFERENCES_PREFERENCE_NODE + "." + mode.name());
			preferences.loadPreferences();
			contextPreferencesByMode.put(mode, preferences);
		}
	}

	public static ContextPreferences get(CompletionMode mode) {
		return contextPreferencesByMode.get(mode);
	}

	private final CompletionMode mode;
	private final String qualifier;
	private final Set<ContextEntryKey> blacklist;
	private final Set<ContextEntryKey> stickylist;
	private final Set<ContextEntryKey> temporaryDisabled;
	private final List<CustomContextEntryData> customContextDataEntries;
	private final List<ContextTypePositionItem> contextTypePositions;

	private ContextPreferences(CompletionMode mode, String qualifier) {
		this.mode = mode;
		this.qualifier = qualifier;
		this.blacklist = new HashSet<>();
		this.stickylist = new HashSet<>();
		this.temporaryDisabled = new HashSet<>();
		this.customContextDataEntries = new ArrayList<>();
		this.contextTypePositions = new ArrayList<>();
	}

	private void loadPreferences() {
		final Preferences preferences = InstanceScope.INSTANCE.getNode(this.qualifier);

		// Load blacklist
		final String blacklistString = preferences.get(BLACKLIST_PREFERENCE_KEY, "");
		for (final String keyString : blacklistString.split(",")) {
			final Optional<ContextEntryKey> optional = ContextEntryKey.parseKeyString(keyString);
			if (optional.isPresent()) {
				this.blacklist.add(optional.get());
			} else {
				AiCoderActivator.log().warn(String.format("Failed to read blacklist key: '%s'", keyString));
			}
		}

		// Load stickylist
		final String stickylistString = preferences.get(STICKYLIST_PREFERENCE_KEY, "");
		for (final String keyString : stickylistString.split(",")) {
			final Optional<ContextEntryKey> optional = ContextEntryKey.parseKeyString(keyString);
			if (optional.isPresent()) {
				this.stickylist.add(optional.get());
			} else {
				AiCoderActivator.log().warn(String.format("Failed to read sticky key: '%s'", keyString));
			}
		}

		// Load user/custom context
		final String userContextString = preferences.get(CUSTOM_CONTEXT_PREFERENCE_KEY, "[]");
		this.customContextDataEntries.addAll(Json.read(userContextString).asJsonList().stream().map(CustomContextEntryData::createFromJson).toList());

		// Load context type positions
		final String contextTypePositionsString = preferences.get(CONTEXT_TYPE_POSITIONS_PREFERENCE_KEY, "[]");
		this.contextTypePositions.addAll(Json.read(contextTypePositionsString).asJsonList().stream().map(ContextTypePositionItem::createFromJson).toList());
	}

	private void savePreferences() {
		final Preferences preferences = InstanceScope.INSTANCE.getNode(this.qualifier);

		// Save blacklist
		final String blacklistString = this.blacklist.stream()
				.map(ContextEntryKey::getKeyString)
				.collect(Collectors.joining(","));
		preferences.put(BLACKLIST_PREFERENCE_KEY, blacklistString);

		// Save stickylist
		final String stickylistString = this.stickylist.stream()
				.map(ContextEntryKey::getKeyString)
				.collect(Collectors.joining(","));
		preferences.put(STICKYLIST_PREFERENCE_KEY, stickylistString);

		// Save user/custom context
		final String userContextString = Json.array(this.customContextDataEntries.stream()
				.map(CustomContextEntryData::toJson)
				.toArray()).toString();
		preferences.put(CUSTOM_CONTEXT_PREFERENCE_KEY, userContextString);

		// Save context type positions
		final String contextTypePositionsString = Json.array(this.contextTypePositions.stream()
				.map(ContextTypePositionItem::toJson)
				.toArray()).toString();
		preferences.put(CONTEXT_TYPE_POSITIONS_PREFERENCE_KEY, contextTypePositionsString);

		try {
			preferences.flush();
		} catch (final Exception exception) {
			AiCoderActivator.log().error("Failed to save preferences", exception);
		}
	}

	public void addToBlacklist(ContextEntryKey entry) {
		this.blacklist.add(entry);
		savePreferences();
	}

	public void removeFromBlacklist(ContextEntryKey entry) {
		this.blacklist.remove(entry);
		savePreferences();
	}

	public boolean isBlacklisted(ContextEntryKey entry) {
		return this.blacklist.contains(entry);
	}

	public void addToStickylist(ContextEntryKey entry) {
		this.stickylist.add(entry);
		savePreferences();
	}

	public void removeFromStickylist(ContextEntryKey entry) {
		this.stickylist.remove(entry);
		savePreferences();
	}

	public boolean isSticky(ContextEntryKey entry) {
		return this.stickylist.contains(entry);
	}

	public Set<ContextEntryKey> getBlacklist() {
		return new HashSet<>(this.blacklist);
	}

	public Set<ContextEntryKey> getStickylist() {
		return new HashSet<>(this.stickylist);
	}

	public List<CustomContextEntryData> getCustomContextEntryDatas() {
		return this.customContextDataEntries;
	}

	public void addToTemporaryDisabled(ContextEntryKey entry) {
		this.temporaryDisabled.add(entry);
	}

	public void setTemporaryDisabled(ContextEntryKey entry, boolean enabled) {
		if (enabled) {
			removeFromTemporaryDisabled(entry);
		} else {
			addToTemporaryDisabled(entry);
		}
	}

	public void removeFromTemporaryDisabled(ContextEntryKey entry) {
		this.temporaryDisabled.remove(entry);
	}

	public boolean isTemporaryDisabled(ContextEntryKey entry) {
		return this.temporaryDisabled.contains(entry);
	}

	public Set<ContextEntryKey> getTemporaryDisabled() {
		return new HashSet<>(this.temporaryDisabled);
	}

	public void setCustomContextEntries(List<CustomContextEntryData> datas) {
		this.customContextDataEntries.clear();
		this.customContextDataEntries.addAll(datas);
		savePreferences();
	}

	public List<ContextTypePositionItem> getContextTypePositions() {
		return this.contextTypePositions;
	}

	public Map<String, ContextTypePositionItem> getContextTypePositionByPrefix() {
		return this.contextTypePositions.stream().collect(Collectors.toMap(ContextTypePositionItem::prefix, Function.identity()));
	}

	public void setContextTypePositions(List<ContextTypePositionItem> newPositions) {
		this.contextTypePositions.clear();
		this.contextTypePositions.addAll(newPositions);
		savePreferences();
	}

	public void setContextTypeEnabled(String contextKey, boolean enabled) {
		if (this.mode == CompletionMode.INLINE && contextKey.startsWith(FillInMiddleContextEntry.PREFIX)) {
			return; // FIM must be always enabled
		}
		this.contextTypePositions.replaceAll(item -> contextKey.startsWith(item.prefix()) ? item.withEnabled(enabled) : item);
		savePreferences();
	}

	public boolean isContextTypeEnabled(String contextKey) {
		return this.contextTypePositions.stream().anyMatch(item -> contextKey.startsWith(item.prefix()) && item.enabled());
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
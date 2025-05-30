package de.hetzge.eclipse.aicoder;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.Preferences;

import de.hetzge.eclipse.aicoder.Context.ContextEntryKey;

public class ContextPreferences {
    private static final String BLACKLIST_PREFERENCE_KEY = "context_blacklist";
    private static final String STICKYLIST_PREFERENCE_KEY = "context_stickylist";
    private static final String PREFERENCES_PREFERENCE_NODE = "de.hetzge.eclipse.aicoder";

    private static final Set<ContextEntryKey> BLACKLIST = new HashSet<>();
    private static final Set<ContextEntryKey> STICKYLIST = new HashSet<>();

    static {
        loadPreferences();
    }

    private static void loadPreferences() {
        final Preferences preferences = InstanceScope.INSTANCE.getNode(PREFERENCES_PREFERENCE_NODE);

        // Load blacklist
        final String blacklistStr = preferences.get(BLACKLIST_PREFERENCE_KEY, "");
        if (!blacklistStr.isEmpty()) {
            for (final String keyString : blacklistStr.split(",")) {
                final Optional<ContextEntryKey> optional = ContextEntryKey.parseKeyString(keyString);
                if(optional.isPresent()) {
                	BLACKLIST.add(optional.get());
                } else {
            		AiCoderActivator.log().warn(String.format("Failed to read blacklist key: '%s'", keyString));
            	}
            }
        }

        // Load stickylist
        final String stickylistStr = preferences.get(STICKYLIST_PREFERENCE_KEY, "");
        if (!stickylistStr.isEmpty()) {
            for (final String keyString : stickylistStr.split(",")) {
            	final Optional<ContextEntryKey> optional = ContextEntryKey.parseKeyString(keyString);
            	if(optional.isPresent()) {
            		STICKYLIST.add(optional.get());
            	} else {
            		AiCoderActivator.log().warn(String.format("Failed to read sticky key: '%s'", keyString));
            	}
            }
        }
    }

    private static void savePreferences() {
        final Preferences prefs = InstanceScope.INSTANCE.getNode(PREFERENCES_PREFERENCE_NODE);

        // Save blacklist
        final String blacklistStr = BLACKLIST.stream()
            .map(ContextEntryKey::getKeyString)
            .collect(Collectors.joining(","));
        prefs.put(BLACKLIST_PREFERENCE_KEY, blacklistStr);

        // Save stickylist
        final String stickylistStr = STICKYLIST.stream()
            .map(ContextEntryKey::getKeyString)
            .collect(Collectors.joining(","));
        prefs.put(STICKYLIST_PREFERENCE_KEY, stickylistStr);

        try {
            prefs.flush();
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
}
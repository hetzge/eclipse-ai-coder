package de.hetzge.eclipse.aicoder;

import java.util.HashSet;
import java.util.Set;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.Preferences;

public class ContextPreferences {
    private static final String BLACKLIST_KEY = "context_blacklist";
    private static final String STICKYLIST_KEY = "context_stickylist";
    private static final String PREFERENCES_NODE = "de.hetzge.eclipse.aicoder";

    private static final Set<String> blacklist = new HashSet<>();
    private static final Set<String> stickylist = new HashSet<>();

    static {
        loadPreferences();
    }

    private static void loadPreferences() {
        Preferences prefs = InstanceScope.INSTANCE.getNode(PREFERENCES_NODE);
        
        // Load blacklist
        String blacklistStr = prefs.get(BLACKLIST_KEY, "");
        if (!blacklistStr.isEmpty()) {
            for (String item : blacklistStr.split(",")) {
                blacklist.add(item);
            }
        }

        // Load stickylist
        String stickylistStr = prefs.get(STICKYLIST_KEY, "");
        if (!stickylistStr.isEmpty()) {
            for (String item : stickylistStr.split(",")) {
                stickylist.add(item);
            }
        }
    }

    private static void savePreferences() {
        Preferences prefs = InstanceScope.INSTANCE.getNode(PREFERENCES_NODE);
        
        // Save blacklist
        String blacklistStr = String.join(",", blacklist);
        prefs.put(BLACKLIST_KEY, blacklistStr);

        // Save stickylist
        String stickylistStr = String.join(",", stickylist);
        prefs.put(STICKYLIST_KEY, stickylistStr);

        try {
            prefs.flush();
        } catch (Exception e) {
            AiCoderActivator.getDefault().getLog().error("Failed to save preferences", e);
        }
    }

    public static void addToBlacklist(String entry) {
        blacklist.add(entry);
        savePreferences();
    }

    public static void removeFromBlacklist(String entry) {
        blacklist.remove(entry);
        savePreferences();
    }

    public static boolean isBlacklisted(String entry) {
        return blacklist.contains(entry);
    }

    public static void addToStickylist(String entry) {
        stickylist.add(entry);
        savePreferences();
    }

    public static void removeFromStickylist(String entry) {
        stickylist.remove(entry);
        savePreferences();
    }

    public static boolean isSticky(String entry) {
        return stickylist.contains(entry);
    }

    public static Set<String> getBlacklist() {
        return new HashSet<>(blacklist);
    }

    public static Set<String> getStickylist() {
        return new HashSet<>(stickylist);
    }
}
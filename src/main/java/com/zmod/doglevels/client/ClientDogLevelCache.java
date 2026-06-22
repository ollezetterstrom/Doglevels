package com.zmod.doglevels.client;

import com.zmod.doglevels.capability.DogBehavior;
import com.zmod.doglevels.network.DogLevelSyncMessage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side cache of dog level data received via {@link DogLevelSyncMessage}.
 *
 * Keyed by wolf entity id. Entries are kept indefinitely while the client is
 * running; the cache is small (one record per tamed wolf the player has ever
 * seen) and is wiped automatically on world unload via {@link #clear()}.
 *
 * Renderers and GUIs read from this cache to avoid inferring the level from
 * the synced MAX_HEALTH attribute (which breaks if the server owner changes
 * health_per_level mid-game, or if other mods also touch the wolf's health).
 */
public final class ClientDogLevelCache
{
    public record Entry(int level, int xp, int xpToNext, int maxLevel, DogBehavior behavior) {
        public float progress() {
            if (xpToNext <= 0) return level >= maxLevel ? 1.0f : 0.0f;
            return Math.max(0.0f, Math.min(1.0f, (float) xp / xpToNext));
        }
        public boolean isMaxLevel() { return level >= maxLevel; }
    }

    private static final Map<Integer, Entry> CACHE = new ConcurrentHashMap<>();

    private ClientDogLevelCache() {}

    public static void update(int entityId, int level, int xp, int xpToNext, int maxLevel, int behaviorOrdinal) {
        Entry e = new Entry(level, xp, xpToNext, maxLevel, DogLevelSyncMessage.behaviorFromOrdinal(behaviorOrdinal));
        CACHE.put(entityId, e);
    }

    public static Entry get(int entityId) {
        return CACHE.get(entityId);
    }

    public static boolean has(int entityId) {
        return CACHE.containsKey(entityId);
    }

    public static void remove(int entityId) {
        CACHE.remove(entityId);
    }

    public static void clear() {
        CACHE.clear();
    }
}

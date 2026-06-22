package com.zmod.doglevels;

import com.zmod.doglevels.capability.DogBehavior;
import com.zmod.doglevels.client.ClientDogLevelCache;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ClientDogLevelCache}.
 *
 * The 1.4.0 fix removed a {@code static} predicted-behavior field from
 * DogScreen that was shared across all dogs. These tests verify the cache
 * itself is correctly keyed per-entity (the architectural replacement for
 * the broken shared state).
 */
class ClientDogLevelCacheTest {

    @AfterEach
    void clearCache() {
        ClientDogLevelCache.clear();
    }

    @Test
    void update_andGet_roundTrip() {
        ClientDogLevelCache.update(100, 5, 10, 30, 30, DogBehavior.AGGRESSIVE.ordinal());
        ClientDogLevelCache.Entry e = ClientDogLevelCache.get(100);
        assertEquals(5, e.level());
        assertEquals(10, e.xp());
        assertEquals(30, e.xpToNext());
        assertEquals(30, e.maxLevel());
        assertEquals(DogBehavior.AGGRESSIVE, e.behavior());
    }

    @Test
    void has_returnsFalseForUnknownEntity() {
        assertFalse(ClientDogLevelCache.has(999));
    }

    @Test
    void has_returnsTrueAfterUpdate() {
        ClientDogLevelCache.update(1, 1, 0, 20, 30, 0);
        assertTrue(ClientDogLevelCache.has(1));
    }

    @Test
    void remove_clearsEntry() {
        ClientDogLevelCache.update(1, 1, 0, 20, 30, 0);
        ClientDogLevelCache.remove(1);
        assertFalse(ClientDogLevelCache.has(1));
        assertNull(ClientDogLevelCache.get(1));
    }

    @Test
    void clear_removesAllEntries() {
        ClientDogLevelCache.update(1, 1, 0, 20, 30, 0);
        ClientDogLevelCache.update(2, 5, 10, 30, 30, 1);
        ClientDogLevelCache.clear();
        assertFalse(ClientDogLevelCache.has(1));
        assertFalse(ClientDogLevelCache.has(2));
    }

    @Test
    void twoDogs_haveIndependentBehavior() {
        // The bug that motivated this cache: DogScreen.predictedBehavior was
        // static, so opening dog B's screen after cycling dog A's button
        // showed A's behavior. The cache is keyed by entity id, so each dog
        // gets its own entry.
        ClientDogLevelCache.update(1, 5, 0, 30, 30, DogBehavior.AGGRESSIVE.ordinal());
        ClientDogLevelCache.update(2, 3, 0, 25, 30, DogBehavior.PASSIVE.ordinal());

        assertEquals(DogBehavior.AGGRESSIVE, ClientDogLevelCache.get(1).behavior());
        assertEquals(DogBehavior.PASSIVE,    ClientDogLevelCache.get(2).behavior());
    }

    @Test
    void entry_progress_isClampedTo01() {
        // xp = 15, xpToNext = 30 → 0.5
        ClientDogLevelCache.update(1, 5, 15, 30, 30, 0);
        assertEquals(0.5f, ClientDogLevelCache.get(1).progress(), 0.001f);

        // xp > xpToNext → clamped to 1.0
        ClientDogLevelCache.update(2, 5, 999, 30, 30, 0);
        assertEquals(1.0f, ClientDogLevelCache.get(2).progress(), 0.001f);
    }

    @Test
    void entry_isMaxLevel_whenLevelAtOrAboveMax() {
        ClientDogLevelCache.update(1, 30, 0, 0, 30, 0);
        assertTrue(ClientDogLevelCache.get(1).isMaxLevel());
    }
}

package com.zmod.doglevels;

import com.zmod.doglevels.capability.DogBehavior;
import com.zmod.doglevels.capability.DogLevelData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link DogBehavior} enum and behavior persistence in {@link DogLevelData}.
 */
class DogBehaviorTest {

    @Test
    void defaultBehavior_isDefault() {
        DogLevelData data = new DogLevelData();
        assertEquals(DogBehavior.DEFAULT, data.getBehavior());
    }

    @Test
    void setBehavior_changesMode() {
        DogLevelData data = new DogLevelData();
        data.setBehavior(DogBehavior.AGGRESSIVE);
        assertEquals(DogBehavior.AGGRESSIVE, data.getBehavior());
        data.setBehavior(DogBehavior.PASSIVE);
        assertEquals(DogBehavior.PASSIVE, data.getBehavior());
    }

    @Test
    void behavior_persistsThroughNbtRoundTrip() {
        DogLevelData data = new DogLevelData();
        data.setBehavior(DogBehavior.AGGRESSIVE);
        data.setLevel(15);
        var tag = data.serializeNBT(null);
        DogLevelData data2 = new DogLevelData();
        data2.deserializeNBT(null, tag);
        assertEquals(DogBehavior.AGGRESSIVE, data2.getBehavior());
        assertEquals(15, data2.getLevel());
    }

    @Test
    void allThreeBehaviorModes_exist() {
        assertEquals(3, DogBehavior.values().length);
        assertTrue(java.util.Arrays.asList(DogBehavior.values()).contains(DogBehavior.DEFAULT));
        assertTrue(java.util.Arrays.asList(DogBehavior.values()).contains(DogBehavior.AGGRESSIVE));
        assertTrue(java.util.Arrays.asList(DogBehavior.values()).contains(DogBehavior.PASSIVE));
    }

    @Test
    void behavior_canBeParsedFromName() {
        assertEquals(DogBehavior.AGGRESSIVE, DogBehavior.valueOf("AGGRESSIVE"));
        assertEquals(DogBehavior.PASSIVE,    DogBehavior.valueOf("PASSIVE"));
        assertEquals(DogBehavior.DEFAULT,    DogBehavior.valueOf("DEFAULT"));
    }

    @Test
    void invalidBehaviorString_throws() {
        assertThrows(IllegalArgumentException.class, () -> DogBehavior.valueOf("INVALID"));
    }

    @Test
    void invalidBehaviorInNbt_fallsBackToDefault() {
        // Serialize a valid AGGRESSIVE behavior, then corrupt the string in the tag.
        DogLevelData data = new DogLevelData();
        data.setBehavior(DogBehavior.AGGRESSIVE);
        var tag = data.serializeNBT(null);
        tag.putString("behavior", "NOT_A_REAL_MODE");

        DogLevelData data2 = new DogLevelData();
        data2.deserializeNBT(null, tag);
        // The deserializer must swallow the bad value and fall back to DEFAULT
        // rather than crashing the world load.
        assertEquals(DogBehavior.DEFAULT, data2.getBehavior());
    }
}

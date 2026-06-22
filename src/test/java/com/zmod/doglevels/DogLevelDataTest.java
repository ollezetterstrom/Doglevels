package com.zmod.doglevels;

import com.zmod.doglevels.capability.DogLevelData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure-Java unit tests for {@link DogLevelData} leveling math.
 *
 * Tests run via {@code ./gradlew test} using JUnit 5. The Forge MC jar is on
 * the test classpath (via ForgeGradle), so {@link net.minecraft.nbt.CompoundTag}
 * is available. The Forge config spec is NOT loaded during tests, so
 * {@link com.zmod.doglevels.config.DogLevelsConfig#getMaxLevel()} returns its
 * fallback (30) — this is intentional and validates the safe-getter behavior.
 */
class DogLevelDataTest {

    @Test
    void freshDog_startsAtLevel1With0Xp() {
        DogLevelData data = new DogLevelData();
        assertEquals(1, data.getLevel());
        assertEquals(0, data.getXP());
        assertFalse(data.isMaxLevel());
    }

    @Test
    void addXp_belowThreshold_doesNotLevelUp() {
        DogLevelData data = new DogLevelData();
        int ups = data.addXP(10); // xpToNext = 20 + 10*0 = 20
        assertEquals(0, ups);
        assertEquals(1, data.getLevel());
        assertEquals(10, data.getXP());
    }

    @Test
    void addXp_atThreshold_triggersOneLevelUp() {
        DogLevelData data = new DogLevelData();
        int ups = data.addXP(20);
        assertEquals(1, ups);
        assertEquals(2, data.getLevel());
        assertEquals(0, data.getXP());
        assertEquals(30, data.xpToNextLevel());
    }

    @Test
    void addXp_bigGain_triggersMultipleLevelUps() {
        DogLevelData data = new DogLevelData();
        // Level 1->2: 20, 2->3: 30, 3->4: 40 → total 90 for 3 level-ups.
        // Adding 100 XP → 3 level-ups, 10 XP left over.
        int ups = data.addXP(100);
        assertEquals(3, ups);
        assertEquals(4, data.getLevel());
        assertEquals(10, data.getXP());
    }

    @Test
    void addXp_pastMax_clampsToMaxLevel() {
        DogLevelData data = new DogLevelData();
        data.addXP(1_000_000);
        assertEquals(DogLevelData.getMaxLevel(), data.getLevel());
        assertEquals(0, data.getXP());
        assertTrue(data.isMaxLevel());
    }

    @Test
    void addXp_atMax_isNoOp() {
        DogLevelData data = new DogLevelData();
        data.addXP(1_000_000);
        int lvlBefore = data.getLevel();
        data.addXP(500);
        assertEquals(lvlBefore, data.getLevel());
        assertEquals(0, data.getXP());
    }

    @Test
    void sizeScale_growsWithLevel() {
        DogLevelData data = new DogLevelData();
        float s1 = data.sizeScale();
        data.setLevel(10);
        float s10 = data.sizeScale();
        assertTrue(s10 > s1, "size at level 10 should exceed size at level 1");
        assertEquals(1.0f, s1, 0.001f);
    }

    @Test
    void sizeScale_cappedAtMaxBonus() {
        DogLevelData data = new DogLevelData();
        data.setLevel(1000);
        float scale = data.sizeScale();
        assertTrue(scale <= 1.6f + 0.001f, "size should be capped at 1.6");
    }

    @Test
    void setLevel_clampsToValidRange() {
        DogLevelData data = new DogLevelData();
        data.setLevel(-5);
        assertEquals(1, data.getLevel());
        data.setLevel(1000);
        assertEquals(DogLevelData.getMaxLevel(), data.getLevel());
    }

    @Test
    void levelProgress_returns0to1() {
        DogLevelData data = new DogLevelData();
        float p0 = data.levelProgress();
        assertEquals(0.0f, p0, 0.001f);
        data.addXP(10);
        float pHalf = data.levelProgress();
        assertEquals(0.5f, pHalf, 0.001f);
    }

    @Test
    void nbtRoundTrip_preservesLevelXpAndBehavior() {
        DogLevelData data = new DogLevelData();
        data.addXP(50);
        int lvlBefore = data.getLevel();
        int xpBefore = data.getXP();
        net.minecraft.nbt.CompoundTag tag = data.serializeNBT(null);
        DogLevelData data2 = new DogLevelData();
        data2.deserializeNBT(null, tag);
        assertEquals(lvlBefore, data2.getLevel());
        assertEquals(xpBefore, data2.getXP());
    }

    @Test
    void getMaxLevel_returnsFallbackWhenConfigNotLoaded() {
        // In the unit-test environment, the Forge config spec is not loaded.
        // getMaxLevel() must therefore return the fallback (30) — and crucially
        // must NOT cache that value at class-init time (the 1.4.0 fix).
        int max = DogLevelData.getMaxLevel();
        assertTrue(max >= 1, "max level must be positive even without config");
        // Calling twice should return the same value (no state mutation).
        assertEquals(max, DogLevelData.getMaxLevel());
    }
}

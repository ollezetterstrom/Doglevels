package com.zmod.doglevels;

import com.zmod.doglevels.abilities.DogAbilities;
import com.zmod.doglevels.capability.DogLevelData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for ability gating logic.
 *
 * Because the Forge config spec is not loaded during tests, the config-backed
 * milestone-level getters return their fallbacks (5/10/15/20/25/30). The
 * has*() methods will return false (config toggles default to false when the
 * spec isn't loaded), so these tests focus on the level-threshold arithmetic
 * and the getter fallbacks.
 */
class DogAbilitiesTest {

    @Test
    void milestoneLevelGetters_returnExpectedFallbacks() {
        // Validates that the getters return sensible fallbacks when the config
        // is not loaded — i.e. the 1.4.0 change from hardcoded constants to
        // config-backed getters doesn't break the default values.
        assertEquals(5,  DogAbilities.splashBiteLevel(),      "Splash Bite fallback");
        assertEquals(10, DogAbilities.fireResistLevel(),      "Fire Resist fallback");
        assertEquals(15, DogAbilities.packHunterLevel(),      "Pack Hunter fallback");
        assertEquals(20, DogAbilities.howlOfVitalityLevel(),  "Howl of Vitality fallback");
        assertEquals(25, DogAbilities.lifestealLevel(),       "Lifesteal fallback");
        assertEquals(30, DogAbilities.bondedEnduranceLevel(), "Bonded Endurance fallback");
    }

    @Test
    void milestoneLevels_areAscending() {
        assertTrue(DogAbilities.splashBiteLevel()     < DogAbilities.fireResistLevel());
        assertTrue(DogAbilities.fireResistLevel()     < DogAbilities.packHunterLevel());
        assertTrue(DogAbilities.packHunterLevel()     < DogAbilities.howlOfVitalityLevel());
        assertTrue(DogAbilities.howlOfVitalityLevel() < DogAbilities.lifestealLevel());
        assertTrue(DogAbilities.lifestealLevel()      < DogAbilities.bondedEnduranceLevel());
    }

    @Test
    void milestoneLevels_withinMaxLevel() {
        int max = DogLevelData.getMaxLevel();
        assertTrue(DogAbilities.splashBiteLevel()      <= max, "Splash Bite <= max");
        assertTrue(DogAbilities.bondedEnduranceLevel() <= max, "Bonded Endurance <= max");
    }

    @Test
    void level4Dog_doesNotMeetSplashBiteThreshold() {
        DogLevelData data = new DogLevelData();
        data.setLevel(4);
        assertTrue(data.getLevel() < DogAbilities.splashBiteLevel());
    }

    @Test
    void level5Dog_meetsSplashBiteThreshold() {
        DogLevelData data = new DogLevelData();
        data.setLevel(5);
        assertTrue(data.getLevel() >= DogAbilities.splashBiteLevel());
    }

    @Test
    void maxLevelDog_meetsAllThresholds() {
        DogLevelData data = new DogLevelData();
        data.setLevel(DogLevelData.getMaxLevel());
        assertTrue(data.getLevel() >= DogAbilities.splashBiteLevel());
        assertTrue(data.getLevel() >= DogAbilities.fireResistLevel());
        assertTrue(data.getLevel() >= DogAbilities.packHunterLevel());
        assertTrue(data.getLevel() >= DogAbilities.howlOfVitalityLevel());
        assertTrue(data.getLevel() >= DogAbilities.lifestealLevel());
        assertTrue(data.getLevel() >= DogAbilities.bondedEnduranceLevel());
    }

    @Test
    void milestoneLevelGetters_areStableAcrossCalls() {
        // Confirms the getters don't mutate state — calling twice returns the
        // same value. (Defends against accidental state caching.)
        int first = DogAbilities.splashBiteLevel();
        int second = DogAbilities.splashBiteLevel();
        assertEquals(first, second);
    }
}

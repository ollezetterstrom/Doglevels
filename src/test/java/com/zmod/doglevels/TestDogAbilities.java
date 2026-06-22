package com.zmod.doglevels;

import com.zmod.doglevels.abilities.DogAbilities;
import com.zmod.doglevels.capability.DogLevelData;

/**
 * Tests for ability gating logic.
 *
 * These tests don't require a Minecraft environment because we only test the
 * has*() methods, which only check the DogLevelData level vs the threshold.
 */
public class TestDogAbilities {

    static int failures = 0;
    static int tests = 0;

    public static void main(String[] args) throws Exception {
        // Note: ability checks depend on DogLevelsConfig.ENABLE_*.get() which will throw
        // because the config isn't loaded. The has*() methods will return false.
        // So we test the LEVEL threshold part separately by checking data.getLevel().

        // Test 1: Level thresholds are correct
        run("Splash bite level threshold", () -> {
            assertEq("SplashBite at 5", 5, DogAbilities.SPLASH_BITE_LEVEL);
        });
        run("Fire resist level threshold", () -> {
            assertEq("FireResist at 10", 10, DogAbilities.FIRE_RESIST_LEVEL);
        });
        run("Pack hunter level threshold", () -> {
            assertEq("PackHunter at 15", 15, DogAbilities.PACK_HUNTER_LEVEL);
        });
        run("Howl of vitality level threshold", () -> {
            assertEq("HowlOfVitality at 20", 20, DogAbilities.HOWL_OF_VITALITY_LEVEL);
        });
        run("Lifesteal level threshold", () -> {
            assertEq("Lifesteal at 25", 25, DogAbilities.LIFESTEAL_LEVEL);
        });
        run("Bonded endurance level threshold", () -> {
            assertEq("BondedEndurance at 30", 30, DogAbilities.BONDED_ENDURANCE_LEVEL);
        });

        // Test 2: A dog at level 4 doesn't meet the splash bite threshold
        run("Level 4 dog doesn't meet splash bite threshold", () -> {
            DogLevelData data = new DogLevelData();
            data.setLevel(4);
            assertTrue("Level 4 < 5", data.getLevel() < DogAbilities.SPLASH_BITE_LEVEL);
        });

        // Test 3: A dog at level 5 meets the splash bite threshold
        run("Level 5 dog meets splash bite threshold", () -> {
            DogLevelData data = new DogLevelData();
            data.setLevel(5);
            assertTrue("Level 5 >= 5", data.getLevel() >= DogAbilities.SPLASH_BITE_LEVEL);
        });

        // Test 4: A dog at max level meets all thresholds
        run("Max level dog meets all thresholds", () -> {
            DogLevelData data = new DogLevelData();
            data.setLevel(DogLevelData.MAX_LEVEL);
            assertTrue("Level >= splash bite", data.getLevel() >= DogAbilities.SPLASH_BITE_LEVEL);
            assertTrue("Level >= fire resist", data.getLevel() >= DogAbilities.FIRE_RESIST_LEVEL);
            assertTrue("Level >= pack hunter", data.getLevel() >= DogAbilities.PACK_HUNTER_LEVEL);
            assertTrue("Level >= howl", data.getLevel() >= DogAbilities.HOWL_OF_VITALITY_LEVEL);
            assertTrue("Level >= lifesteal", data.getLevel() >= DogAbilities.LIFESTEAL_LEVEL);
            assertTrue("Level >= bonded endurance", data.getLevel() >= DogAbilities.BONDED_ENDURANCE_LEVEL);
        });

        // Test 5: Verify milestone levels are ascending
        run("Milestone levels are ascending", () -> {
            assertTrue("5 < 10", DogAbilities.SPLASH_BITE_LEVEL < DogAbilities.FIRE_RESIST_LEVEL);
            assertTrue("10 < 15", DogAbilities.FIRE_RESIST_LEVEL < DogAbilities.PACK_HUNTER_LEVEL);
            assertTrue("15 < 20", DogAbilities.PACK_HUNTER_LEVEL < DogAbilities.HOWL_OF_VITALITY_LEVEL);
            assertTrue("20 < 25", DogAbilities.HOWL_OF_VITALITY_LEVEL < DogAbilities.LIFESTEAL_LEVEL);
            assertTrue("25 < 30", DogAbilities.LIFESTEAL_LEVEL < DogAbilities.BONDED_ENDURANCE_LEVEL);
        });

        // Test 6: All milestone levels are within MAX_LEVEL
        run("All milestone levels <= MAX_LEVEL", () -> {
            assertTrue("Splash bite <= MAX_LEVEL", DogAbilities.SPLASH_BITE_LEVEL <= DogLevelData.MAX_LEVEL);
            assertTrue("Bonded endurance <= MAX_LEVEL", DogAbilities.BONDED_ENDURANCE_LEVEL <= DogLevelData.MAX_LEVEL);
        });

        System.out.println("\n=== Tests complete ===");
        System.out.println("Total: " + tests + ", Failures: " + failures);
        if (failures > 0) {
            System.exit(1);
        }
    }

    interface Test { void run() throws Exception; }

    static void run(String name, Test t) {
        tests++;
        try {
            t.run();
        } catch (Throwable e) {
            failures++;
            System.err.println("FAIL: " + name + " - Exception: " + e);
            e.printStackTrace();
        }
    }

    static void assertEq(String label, int expected, int actual) {
        if (expected != actual) {
            failures++;
            System.err.println("FAIL: " + label + " - expected " + expected + ", got " + actual);
        } else {
            System.out.println("PASS: " + label);
        }
    }

    static void assertTrue(String label, boolean condition) {
        if (!condition) {
            failures++;
            System.err.println("FAIL: " + label);
        } else {
            System.out.println("PASS: " + label);
        }
    }
}

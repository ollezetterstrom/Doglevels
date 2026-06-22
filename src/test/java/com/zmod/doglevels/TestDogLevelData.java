package com.zmod.doglevels;

import com.zmod.doglevels.capability.DogLevelData;
import net.minecraft.nbt.CompoundTag;

/**
 * Pure-Java unit tests for DogLevelData logic.
 *
 * Tests:
 *   - Fresh dog starts at level 1, 0 XP
 *   - XP below threshold doesn't level up
 *   - XP at threshold triggers level-up
 *   - Multiple level-ups from one XP gain
 *   - Leveling stops at MAX_LEVEL
 *   - XP at max level is a no-op
 *   - sizeScale grows with level, capped at MAX_SIZE_BONUS
 *   - setLevel clamps to valid range
 *   - levelProgress returns 0-1
 *   - NBT round-trip preserves state
 *
 * NOTE: This requires the Forge MC jar on the classpath because DogLevelData
 * uses net.minecraft.nbt.CompoundTag. We pass null as the HolderLookup.Provider
 * to serializeNBT/deserializeNBT because we don't use any registry-typed data.
 */
public class TestDogLevelData {

    static int failures = 0;
    static int tests = 0;

    public static void main(String[] args) throws Exception {

        // Test 1: Fresh dog starts at level 1, 0 XP
        run("Fresh dog starts at level 1, 0 XP", () -> {
            DogLevelData data = new DogLevelData();
            assertEq("Level 1 fresh", 1, data.getLevel());
            assertEq("XP 0 fresh", 0, data.getXP());
            assertEq("Not max level", false, data.isMaxLevel());
        });

        // Test 2: Adding XP below threshold doesn't level up
        run("Adding XP below threshold doesn't level up", () -> {
            DogLevelData data = new DogLevelData();
            int ups = data.addXP(10);  // xpToNext = 20 + 10*0 = 20
            assertEq("No level up from 10 XP", 0, ups);
            assertEq("Level still 1", 1, data.getLevel());
            assertEq("XP stored", 10, data.getXP());
        });

        // Test 3: Adding enough XP triggers one level-up
        run("Adding enough XP triggers one level-up", () -> {
            DogLevelData data = new DogLevelData();
            int ups = data.addXP(20);  // exactly enough for level 2
            assertEq("One level up", 1, ups);
            assertEq("Level 2", 2, data.getLevel());
            assertEq("XP 0 after level up", 0, data.getXP());
            assertEq("Next level requires 30", 30, data.xpToNextLevel());
        });

        // Test 4: Multiple level-ups from a big XP gain
        run("Multiple level-ups from a big XP gain", () -> {
            DogLevelData data = new DogLevelData();
            // Level 1->2: 20, 2->3: 30, 3->4: 40
            // Total for 3 levels: 90
            // Adding 100 XP: 3 level-ups, 10 XP left
            int ups = data.addXP(100);
            assertEq("Three level ups from 100 XP", 3, ups);
            assertEq("Level 4", 4, data.getLevel());
            assertEq("XP remainder 10", 10, data.getXP());
        });

        // Test 5: Leveling to max stops at max
        run("Leveling to max stops at max", () -> {
            DogLevelData data = new DogLevelData();
            data.addXP(1_000_000);
            assertEq("Max level reached", DogLevelData.MAX_LEVEL, data.getLevel());
            assertEq("XP 0 at max", 0, data.getXP());
            assertEq("Is max level", true, data.isMaxLevel());
        });

        // Test 6: Adding XP at max level is a no-op
        run("Adding XP at max level is a no-op", () -> {
            DogLevelData data = new DogLevelData();
            data.addXP(1_000_000);
            int lvlBefore = data.getLevel();
            data.addXP(500);
            assertEq("Still max level", lvlBefore, data.getLevel());
            assertEq("XP still 0 at max", 0, data.getXP());
        });

        // Test 7: sizeScale grows with level
        run("sizeScale grows with level", () -> {
            DogLevelData data = new DogLevelData();
            float s1 = data.sizeScale();
            data.setLevel(10);
            float s10 = data.sizeScale();
            assertTrue("Size at level 10 > size at level 1", s10 > s1);
            assertTrue("Size at level 1 is 1.0", Math.abs(s1 - 1.0f) < 0.001f);
        });

        // Test 8: sizeScale caps at MAX_SIZE_BONUS
        run("sizeScale caps at MAX_SIZE_BONUS", () -> {
            DogLevelData data = new DogLevelData();
            data.setLevel(1000);
            float scale = data.sizeScale();
            assertTrue("Size capped at 1.6", scale <= 1.6f + 0.001f);
        });

        // Test 9: setLevel clamps to valid range
        run("setLevel clamps to valid range", () -> {
            DogLevelData data = new DogLevelData();
            data.setLevel(-5);
            assertEq("Negative level clamped to 1", 1, data.getLevel());
            data.setLevel(1000);
            assertEq("Too-high level clamped to max", DogLevelData.MAX_LEVEL, data.getLevel());
        });

        // Test 10: levelProgress returns 0-1
        run("levelProgress returns 0-1", () -> {
            DogLevelData data = new DogLevelData();
            float p0 = data.levelProgress();
            assertTrue("Progress at 0 XP is 0", Math.abs(p0) < 0.001f);
            data.addXP(10);
            float pHalf = data.levelProgress();
            assertTrue("Progress at 10/20 XP is 0.5", Math.abs(pHalf - 0.5f) < 0.001f);
        });

        // Test 11: NBT round-trip preserves level and XP
        run("NBT round-trip preserves level and XP", () -> {
            DogLevelData data = new DogLevelData();
            data.addXP(50);
            int lvlBefore = data.getLevel();
            int xpBefore = data.getXP();
            CompoundTag tag = data.serializeNBT(null);
            DogLevelData data2 = new DogLevelData();
            data2.deserializeNBT(null, tag);
            assertEq("NBT round-trip level", lvlBefore, data2.getLevel());
            assertEq("NBT round-trip XP", xpBefore, data2.getXP());
        });

        System.out.println("\n=== Tests complete ===");
        System.out.println("Total: " + tests + ", Failures: " + failures);
        if (failures > 0) {
            System.exit(1);
        }
    }

    interface Test {
        void run() throws Exception;
    }

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

    static void assertEq(String label, boolean expected, boolean actual) {
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

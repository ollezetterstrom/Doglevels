package com.zmod.doglevels;

import com.zmod.doglevels.capability.DogBehavior;
import com.zmod.doglevels.capability.DogLevelData;

/**
 * Tests for DogBehavior enum and behavior persistence in DogLevelData.
 */
public class TestDogBehavior {

    static int failures = 0;
    static int tests = 0;

    public static void main(String[] args) throws Exception {

        // Test 1: Default behavior is DEFAULT
        run("Default behavior is DEFAULT", () -> {
            DogLevelData data = new DogLevelData();
            assertEq("Default behavior", DogBehavior.DEFAULT, data.getBehavior());
        });

        // Test 2: setBehavior changes the mode
        run("setBehavior changes the mode", () -> {
            DogLevelData data = new DogLevelData();
            data.setBehavior(DogBehavior.AGGRESSIVE);
            assertEq("Behavior is AGGRESSIVE", DogBehavior.AGGRESSIVE, data.getBehavior());
            data.setBehavior(DogBehavior.PASSIVE);
            assertEq("Behavior is PASSIVE", DogBehavior.PASSIVE, data.getBehavior());
        });

        // Test 3: Behavior persists through NBT round-trip
        run("Behavior persists through NBT round-trip", () -> {
            DogLevelData data = new DogLevelData();
            data.setBehavior(DogBehavior.AGGRESSIVE);
            data.setLevel(15);
            var tag = data.serializeNBT(null);
            DogLevelData data2 = new DogLevelData();
            data2.deserializeNBT(null, tag);
            assertEq("Behavior after reload", DogBehavior.AGGRESSIVE, data2.getBehavior());
            assertEq("Level after reload", 15, data2.getLevel());
        });

        // Test 4: All three behavior modes exist
        run("All three behavior modes exist", () -> {
            assertEq("3 modes", 3, DogBehavior.values().length);
            assertTrue("DEFAULT exists", java.util.Arrays.asList(DogBehavior.values()).contains(DogBehavior.DEFAULT));
            assertTrue("AGGRESSIVE exists", java.util.Arrays.asList(DogBehavior.values()).contains(DogBehavior.AGGRESSIVE));
            assertTrue("PASSIVE exists", java.util.Arrays.asList(DogBehavior.values()).contains(DogBehavior.PASSIVE));
        });

        // Test 5: Behavior can be set from string name (command compatibility)
        run("Behavior can be set from string name", () -> {
            DogBehavior mode = DogBehavior.valueOf("AGGRESSIVE");
            assertEq("Parsed AGGRESSIVE", DogBehavior.AGGRESSIVE, mode);
            mode = DogBehavior.valueOf("PASSIVE");
            assertEq("Parsed PASSIVE", DogBehavior.PASSIVE, mode);
            mode = DogBehavior.valueOf("DEFAULT");
            assertEq("Parsed DEFAULT", DogBehavior.DEFAULT, mode);
        });

        // Test 6: Invalid behavior string throws (command error handling)
        run("Invalid behavior string throws", () -> {
            try {
                DogBehavior.valueOf("INVALID");
                failures++;
                System.err.println("FAIL: Should have thrown for INVALID");
            } catch (IllegalArgumentException e) {
                System.out.println("PASS: Throws for invalid mode");
            }
        });

        System.out.println("\n=== Tests complete ===");
        System.out.println("Total: " + tests + ", Failures: " + failures);
        if (failures > 0) System.exit(1);
    }

    interface Test { void run() throws Exception; }
    static void run(String name, Test t) {
        tests++;
        try { t.run(); }
        catch (Throwable e) { failures++; System.err.println("FAIL: " + name + " - " + e); }
    }
    static void assertEq(String label, Object expected, Object actual) {
        if (!expected.equals(actual)) { failures++; System.err.println("FAIL: " + label + " - expected " + expected + ", got " + actual); }
        else System.out.println("PASS: " + label);
    }
    static void assertEq(String label, int expected, int actual) {
        if (expected != actual) { failures++; System.err.println("FAIL: " + label + " - expected " + expected + ", got " + actual); }
        else System.out.println("PASS: " + label);
    }
    static void assertTrue(String label, boolean condition) {
        if (!condition) { failures++; System.err.println("FAIL: " + label); }
        else System.out.println("PASS: " + label);
    }
}

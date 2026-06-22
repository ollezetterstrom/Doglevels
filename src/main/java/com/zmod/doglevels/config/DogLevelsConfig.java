package com.zmod.doglevels.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * All configurable values for the Dog Levels mod.
 *
 * IMPORTANT: the static `MAX_LEVEL` IntValue is read live via {@link #getMaxLevel()}
 * at runtime — do NOT cache it in a `static final` field anywhere. Previous versions
 * cached the value at class-init time, which meant config changes had no effect until
 * a full game restart (and even then, only if the class wasn't already loaded).
 *
 * All safe-getter helpers (getInt / getDouble / getBool) swallow config-load errors
 * and return a fallback, so callers don't need to repeat the try/catch boilerplate.
 */
public final class DogLevelsConfig
{
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    // ---- leveling ----
    public static final ForgeConfigSpec.IntValue MAX_LEVEL;
    public static final ForgeConfigSpec.DoubleValue XP_PER_LEVEL_BASE;
    public static final ForgeConfigSpec.DoubleValue XP_PER_LEVEL_GROWTH;
    public static final ForgeConfigSpec.DoubleValue XP_FROM_KILL_HEALTH_MULT;
    public static final ForgeConfigSpec.DoubleValue XP_FROM_PLAYER_KILL_MULT;
    public static final ForgeConfigSpec.IntValue XP_FROM_TREAT;

    // ---- stats per level ----
    public static final ForgeConfigSpec.DoubleValue HEALTH_PER_LEVEL;
    public static final ForgeConfigSpec.DoubleValue DAMAGE_PER_LEVEL;
    public static final ForgeConfigSpec.DoubleValue SPEED_PER_LEVEL;
    public static final ForgeConfigSpec.DoubleValue ARMOR_PER_LEVEL;
    public static final ForgeConfigSpec.DoubleValue KNOCKBACK_RESIST_PER_LEVEL;

    // ---- size ----
    public static final ForgeConfigSpec.DoubleValue SIZE_PER_LEVEL;
    public static final ForgeConfigSpec.DoubleValue MAX_SIZE_BONUS;

    // ---- ability unlock levels (configurable in 1.4.0) ----
    public static final ForgeConfigSpec.IntValue SPLASH_BITE_LEVEL;
    public static final ForgeConfigSpec.IntValue FIRE_RESIST_LEVEL;
    public static final ForgeConfigSpec.IntValue PACK_HUNTER_LEVEL;
    public static final ForgeConfigSpec.IntValue HOWL_OF_VITALITY_LEVEL;
    public static final ForgeConfigSpec.IntValue LIFESTEAL_LEVEL;
    public static final ForgeConfigSpec.IntValue BONDED_ENDURANCE_LEVEL;

    // ---- ability tuning (configurable in 1.4.0) ----
    public static final ForgeConfigSpec.DoubleValue SPLASH_BITE_RADIUS;
    public static final ForgeConfigSpec.DoubleValue SPLASH_BITE_DAMAGE_MULT;
    public static final ForgeConfigSpec.IntValue    PACK_HUNTER_PERIOD_TICKS;
    public static final ForgeConfigSpec.DoubleValue PACK_HUNTER_RADIUS;
    public static final ForgeConfigSpec.IntValue    PACK_HUNTER_BUFF_TICKS;
    public static final ForgeConfigSpec.DoubleValue HOWL_OF_VITALITY_CHANCE;
    public static final ForgeConfigSpec.DoubleValue HOWL_OF_VITALITY_HEAL;
    public static final ForgeConfigSpec.DoubleValue LIFESTEAL_PERCENT;
    public static final ForgeConfigSpec.IntValue    FIRE_RESIST_REFRESH_TICKS;
    public static final ForgeConfigSpec.IntValue    BONDED_ENDURANCE_REFRESH_TICKS;

    // ---- behavior ----
    public static final ForgeConfigSpec.DoubleValue AGGRESSIVE_RANGE;

    // ---- ability toggles ----
    public static final ForgeConfigSpec.BooleanValue ENABLE_SPLASH_BITE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_FIRE_RESIST;
    public static final ForgeConfigSpec.BooleanValue ENABLE_PACK_HUNTER;
    public static final ForgeConfigSpec.BooleanValue ENABLE_HOWL_OF_VITALITY;
    public static final ForgeConfigSpec.BooleanValue ENABLE_LIFESTEAL;
    public static final ForgeConfigSpec.BooleanValue ENABLE_BONDED_ENDURANCE;

    static {
        BUILDER.push("leveling");

        MAX_LEVEL = BUILDER
                .comment("Maximum level a dog can reach.", "Default: 30")
                .defineInRange("max_level", 30, 1, 1000);

        XP_PER_LEVEL_BASE = BUILDER
                .comment("Base XP required to go from level 1 to 2.", "Default: 20.0")
                .defineInRange("xp_per_level_base", 20.0, 1.0, 1_000_000.0);

        XP_PER_LEVEL_GROWTH = BUILDER
                .comment("How much extra XP is required per level beyond the base.", "Default: 10.0")
                .defineInRange("xp_per_level_growth", 10.0, 0.0, 1_000_000.0);

        XP_FROM_KILL_HEALTH_MULT = BUILDER
                .comment("Multiplier applied to a killed entity's max health to determine XP granted",
                        "when the DOG lands the killing blow.", "Default: 1.0")
                .defineInRange("xp_from_kill_health_mult", 1.0, 0.0, 100.0);

        XP_FROM_PLAYER_KILL_MULT = BUILDER
                .comment("Multiplier for ASSIST XP: when the PLAYER kills a mob, all tamed dogs",
                        "within 32 blocks get XP = victim_max_health * xp_from_kill_health_mult * this value.",
                        "Set to 0 to disable assist XP (only dog-kills grant XP).",
                        "Default: 0.5 (50% of full kill XP)")
                .defineInRange("xp_from_player_kill_mult", 0.5, 0.0, 100.0);

        XP_FROM_TREAT = BUILDER
                .comment("XP granted by feeding a dog a Treat.", "Default: 50")
                .defineInRange("xp_from_treat", 50, 0, 100_000);

        BUILDER.pop();

        BUILDER.push("stats_per_level");
        HEALTH_PER_LEVEL = BUILDER.comment("Bonus max health per level.", "Default: 1.0")
                .defineInRange("health_per_level", 1.0, 0.0, 100.0);
        DAMAGE_PER_LEVEL = BUILDER.comment("Bonus attack damage per level.", "Default: 0.5")
                .defineInRange("damage_per_level", 0.5, 0.0, 100.0);
        SPEED_PER_LEVEL = BUILDER.comment("Bonus movement speed per level.", "Default: 0.002")
                .defineInRange("speed_per_level", 0.002, 0.0, 1.0);
        ARMOR_PER_LEVEL = BUILDER.comment("Bonus armor per level.", "Default: 0.5")
                .defineInRange("armor_per_level", 0.5, 0.0, 100.0);
        KNOCKBACK_RESIST_PER_LEVEL = BUILDER.comment("Bonus knockback resistance per level.", "Default: 0.02")
                .defineInRange("knockback_resist_per_level", 0.02, 0.0, 1.0);
        BUILDER.pop();

        BUILDER.push("size");
        SIZE_PER_LEVEL = BUILDER.comment("How much the dog's visual size increases per level.", "Default: 0.015")
                .defineInRange("size_per_level", 0.015, 0.0, 1.0);
        MAX_SIZE_BONUS = BUILDER.comment("Maximum total size bonus.", "Default: 0.6")
                .defineInRange("max_size_bonus", 0.6, 0.0, 5.0);
        BUILDER.pop();

        BUILDER.push("ability_levels");
        SPLASH_BITE_LEVEL        = BUILDER.comment("Level at which Splash Bite unlocks.", "Default: 5")
                .defineInRange("splash_bite_level", 5, 1, 1000);
        FIRE_RESIST_LEVEL        = BUILDER.comment("Level at which Fire Resistance unlocks.", "Default: 10")
                .defineInRange("fire_resist_level", 10, 1, 1000);
        PACK_HUNTER_LEVEL        = BUILDER.comment("Level at which Pack Hunter unlocks.", "Default: 15")
                .defineInRange("pack_hunter_level", 15, 1, 1000);
        HOWL_OF_VITALITY_LEVEL   = BUILDER.comment("Level at which Howl of Vitality unlocks.", "Default: 20")
                .defineInRange("howl_of_vitality_level", 20, 1, 1000);
        LIFESTEAL_LEVEL          = BUILDER.comment("Level at which Lifesteal unlocks.", "Default: 25")
                .defineInRange("lifesteal_level", 25, 1, 1000);
        BONDED_ENDURANCE_LEVEL   = BUILDER.comment("Level at which Bonded Endurance unlocks.", "Default: 30")
                .defineInRange("bonded_endurance_level", 30, 1, 1000);
        BUILDER.pop();

        BUILDER.push("ability_tuning");
        SPLASH_BITE_RADIUS         = BUILDER.comment("Radius (blocks) for Splash Bite's splash damage.", "Default: 2.5")
                .defineInRange("splash_bite_radius", 2.5, 0.5, 16.0);
        SPLASH_BITE_DAMAGE_MULT    = BUILDER.comment("Multiplier applied to primary damage for Splash Bite splash.", "Default: 0.5 (50%)")
                .defineInRange("splash_bite_damage_mult", 0.5, 0.0, 5.0);

        PACK_HUNTER_PERIOD_TICKS   = BUILDER.comment("How often (ticks) Pack Hunter re-applies its Strength buff.", "Default: 100 (5s)")
                .defineInRange("pack_hunter_period_ticks", 100, 20, 6000);
        PACK_HUNTER_RADIUS         = BUILDER.comment("Radius (blocks) for Pack Hunter's buff spread.", "Default: 8.0")
                .defineInRange("pack_hunter_radius", 8.0, 1.0, 64.0);
        PACK_HUNTER_BUFF_TICKS     = BUILDER.comment("Duration (ticks) of the Strength buff applied by Pack Hunter.", "Default: 120 (6s)")
                .defineInRange("pack_hunter_buff_ticks", 120, 20, 6000);

        HOWL_OF_VITALITY_CHANCE    = BUILDER.comment("Chance (0-1) on each dog attack to trigger Howl of Vitality.", "Default: 0.2 (20%)")
                .defineInRange("howl_of_vitality_chance", 0.2, 0.0, 1.0);
        HOWL_OF_VITALITY_HEAL      = BUILDER.comment("Amount healed on Howl of Vitality trigger.", "Default: 1.0")
                .defineInRange("howl_of_vitality_heal", 1.0, 0.0, 100.0);

        LIFESTEAL_PERCENT          = BUILDER.comment("Fraction of damage dealt that is healed to the dog by Lifesteal.", "Default: 0.25 (25%)")
                .defineInRange("lifesteal_percent", 0.25, 0.0, 5.0);

        FIRE_RESIST_REFRESH_TICKS  = BUILDER.comment("Duration (ticks) of each Fire Resistance refresh.", "Default: 400 (20s)")
                .defineInRange("fire_resist_refresh_ticks", 20, 20, 6000);
        BONDED_ENDURANCE_REFRESH_TICKS = BUILDER.comment("Duration (ticks) of each Regeneration refresh.", "Default: 200 (10s)")
                .defineInRange("bonded_endurance_refresh_ticks", 20, 20, 6000);
        BUILDER.pop();

        BUILDER.push("behavior");
        AGGRESSIVE_RANGE = BUILDER.comment("Range (blocks) at which AGGRESSIVE dogs auto-target hostiles.", "Default: 16.0")
                .defineInRange("aggressive_range", 16.0, 1.0, 64.0);
        BUILDER.pop();

        BUILDER.push("abilities");
        ENABLE_SPLASH_BITE = BUILDER.comment("Unlock Splash Bite.", "Default: true")
                .define("enable_splash_bite", true);
        ENABLE_FIRE_RESIST = BUILDER.comment("Unlock Fire Resistance.", "Default: true")
                .define("enable_fire_resist", true);
        ENABLE_PACK_HUNTER = BUILDER.comment("Unlock Pack Hunter.", "Default: true")
                .define("enable_pack_hunter", true);
        ENABLE_HOWL_OF_VITALITY = BUILDER.comment("Unlock Howl of Vitality.", "Default: true")
                .define("enable_howl_of_vitality", true);
        ENABLE_LIFESTEAL = BUILDER.comment("Unlock Lifesteal.", "Default: true")
                .define("enable_lifesteal", true);
        ENABLE_BONDED_ENDURANCE = BUILDER.comment("Unlock Bonded Endurance.", "Default: true")
                .define("enable_bonded_endurance", true);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    private DogLevelsConfig() {}

    // ---------- safe getters (never throw, even if config not yet loaded) ----------

    public static int getMaxLevel() { return getInt(MAX_LEVEL, 30); }

    public static int getInt(ForgeConfigSpec.IntValue v, int fallback) {
        try { return v != null && v.get() != null ? v.get() : fallback; }
        catch (Throwable ignored) { return fallback; }
    }

    public static double getDouble(ForgeConfigSpec.DoubleValue v, double fallback) {
        try { return v != null && v.get() != null ? v.get() : fallback; }
        catch (Throwable ignored) { return fallback; }
    }

    public static boolean getBool(ForgeConfigSpec.BooleanValue v, boolean fallback) {
        try { return v != null && v.get() != null ? v.get() : fallback; }
        catch (Throwable ignored) { return fallback; }
    }
}

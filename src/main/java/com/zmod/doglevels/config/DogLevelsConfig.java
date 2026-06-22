package com.zmod.doglevels.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class DogLevelsConfig
{
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.IntValue MAX_LEVEL;
    public static final ForgeConfigSpec.DoubleValue XP_PER_LEVEL_BASE;
    public static final ForgeConfigSpec.DoubleValue XP_PER_LEVEL_GROWTH;
    public static final ForgeConfigSpec.DoubleValue XP_FROM_KILL_HEALTH_MULT;
    public static final ForgeConfigSpec.DoubleValue XP_FROM_PLAYER_KILL_MULT;
    public static final ForgeConfigSpec.IntValue XP_FROM_TREAT;
    public static final ForgeConfigSpec.DoubleValue HEALTH_PER_LEVEL;
    public static final ForgeConfigSpec.DoubleValue DAMAGE_PER_LEVEL;
    public static final ForgeConfigSpec.DoubleValue SPEED_PER_LEVEL;
    public static final ForgeConfigSpec.DoubleValue ARMOR_PER_LEVEL;
    public static final ForgeConfigSpec.DoubleValue KNOCKBACK_RESIST_PER_LEVEL;
    public static final ForgeConfigSpec.DoubleValue SIZE_PER_LEVEL;
    public static final ForgeConfigSpec.DoubleValue MAX_SIZE_BONUS;
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
                .comment("Multiplier applied to a killed entity's max health to determine XP granted", "when the DOG lands the killing blow.", "Default: 1.0")
                .defineInRange("xp_from_kill_health_mult", 1.0, 0.0, 100.0);

        XP_FROM_PLAYER_KILL_MULT = BUILDER
                .comment("Multiplier for ASSIST XP: when the PLAYER kills a mob, all tamed dogs", "within 32 blocks get XP = victim_max_health * xp_from_kill_health_mult * this value.", "Set to 0 to disable assist XP (only dog-kills grant XP).", "Default: 0.5 (50% of full kill XP)")
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

        BUILDER.push("abilities");
        ENABLE_SPLASH_BITE = BUILDER.comment("Unlock Splash Bite at level 5.", "Default: true")
                .define("enable_splash_bite", true);
        ENABLE_FIRE_RESIST = BUILDER.comment("Unlock Fire Resistance at level 10.", "Default: true")
                .define("enable_fire_resist", true);
        ENABLE_PACK_HUNTER = BUILDER.comment("Unlock Pack Hunter at level 15.", "Default: true")
                .define("enable_pack_hunter", true);
        ENABLE_HOWL_OF_VITALITY = BUILDER.comment("Unlock Howl of Vitality at level 20.", "Default: true")
                .define("enable_howl_of_vitality", true);
        ENABLE_LIFESTEAL = BUILDER.comment("Unlock Lifesteal at level 25.", "Default: true")
                .define("enable_lifesteal", true);
        ENABLE_BONDED_ENDURANCE = BUILDER.comment("Unlock Bonded Endurance at level 30.", "Default: true")
                .define("enable_bonded_endurance", true);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}

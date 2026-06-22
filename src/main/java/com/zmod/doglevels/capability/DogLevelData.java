package com.zmod.doglevels.capability;

import com.zmod.doglevels.DogLevelsMod;
import com.zmod.doglevels.config.DogLevelsConfig;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.util.INBTSerializable;

/**
 * Per-dog leveling data, persisted via INBTSerializable.
 *
 * Stats are applied as ADDITIVE AttributeModifiers (not by overwriting base values).
 * This preserves vanilla MC's base attribute values (which differ between wild and
 * tamed wolves, and between wolf variants) and only adds our level bonus on top.
 *
 * Also stores the dog's behavior mode (DEFAULT, AGGRESSIVE, PASSIVE).
 */
public final class DogLevelData implements INBTSerializable<CompoundTag>
{
    public static final int MAX_LEVEL;
    static {
        int max;
        try {
            max = DogLevelsConfig.MAX_LEVEL != null && DogLevelsConfig.MAX_LEVEL.get() != null
                    ? DogLevelsConfig.MAX_LEVEL.get() : 30;
        } catch (Throwable ignored) {
            max = 30;
        }
        MAX_LEVEL = max;
    }

    public static final ResourceLocation HEALTH_MOD_ID =
            ResourceLocation.fromNamespaceAndPath(DogLevelsMod.MOD_ID, "health_bonus");
    public static final ResourceLocation DAMAGE_MOD_ID =
            ResourceLocation.fromNamespaceAndPath(DogLevelsMod.MOD_ID, "damage_bonus");
    public static final ResourceLocation SPEED_MOD_ID =
            ResourceLocation.fromNamespaceAndPath(DogLevelsMod.MOD_ID, "speed_bonus");
    public static final ResourceLocation ARMOR_MOD_ID =
            ResourceLocation.fromNamespaceAndPath(DogLevelsMod.MOD_ID, "armor_bonus");
    public static final ResourceLocation KB_MOD_ID =
            ResourceLocation.fromNamespaceAndPath(DogLevelsMod.MOD_ID, "kb_bonus");

    private int level = 1;
    private int xp = 0;
    private boolean statsApplied = false;
    private DogBehavior behavior = DogBehavior.DEFAULT;

    public int getLevel() { return level; }
    public int getXP() { return xp; }
    public DogBehavior getBehavior() { return behavior; }

    public void setLevel(int lvl) { this.level = Math.max(1, Math.min(MAX_LEVEL, lvl)); }
    public void setXP(int amount) { this.xp = Math.max(0, amount); }
    public boolean isStatsApplied() { return statsApplied; }
    public void setStatsApplied(boolean applied) { this.statsApplied = applied; }
    public void setBehavior(DogBehavior b) { this.behavior = b; }

    public int addXP(int amount)
    {
        if (level >= MAX_LEVEL) {
            this.xp = 0;
            return 0;
        }
        if (amount <= 0) return 0;
        this.xp += amount;
        int levelUps = 0;
        while (level < MAX_LEVEL && this.xp >= xpToNextLevel()) {
            this.xp -= xpToNextLevel();
            this.level++;
            levelUps++;
        }
        if (level >= MAX_LEVEL) {
            this.xp = 0;
        }
        return levelUps;
    }

    public int xpToNextLevel()
    {
        if (level >= MAX_LEVEL) return 0;
        double base = cfg(DogLevelsConfig.XP_PER_LEVEL_BASE, 20.0);
        double growth = cfg(DogLevelsConfig.XP_PER_LEVEL_GROWTH, 10.0);
        return (int) Math.round(base + growth * (level - 1));
    }

    public float levelProgress()
    {
        int need = xpToNextLevel();
        if (need <= 0) return 1.0f;
        return Math.max(0.0f, Math.min(1.0f, (float) xp / need));
    }

    public boolean isMaxLevel() { return level >= MAX_LEVEL; }

    public float sizeScale()
    {
        double perLevel = cfg(DogLevelsConfig.SIZE_PER_LEVEL, 0.015);
        double maxBonus = cfg(DogLevelsConfig.MAX_SIZE_BONUS, 0.6);
        double bonus = Math.min(maxBonus, perLevel * (level - 1));
        return (float) (1.0 + bonus);
    }

    public void applyStats(Wolf wolf)
    {
        if (wolf == null) return;

        double healthBonus = (level - 1) * cfg(DogLevelsConfig.HEALTH_PER_LEVEL, 1.0);
        double damageBonus = (level - 1) * cfg(DogLevelsConfig.DAMAGE_PER_LEVEL, 0.5);
        double speedBonus  = (level - 1) * cfg(DogLevelsConfig.SPEED_PER_LEVEL, 0.002);
        double armorBonus  = (level - 1) * cfg(DogLevelsConfig.ARMOR_PER_LEVEL, 0.5);
        double kbBonus     = (level - 1) * cfg(DogLevelsConfig.KNOCKBACK_RESIST_PER_LEVEL, 0.02);

        applyModifier(wolf, Attributes.MAX_HEALTH, HEALTH_MOD_ID, healthBonus);
        applyModifier(wolf, Attributes.ATTACK_DAMAGE, DAMAGE_MOD_ID, damageBonus);
        applyModifier(wolf, Attributes.MOVEMENT_SPEED, SPEED_MOD_ID, speedBonus);
        applyModifier(wolf, Attributes.ARMOR, ARMOR_MOD_ID, armorBonus);
        applyModifier(wolf, Attributes.KNOCKBACK_RESISTANCE, KB_MOD_ID, kbBonus);

        if (wolf.getHealth() > wolf.getMaxHealth()) {
            wolf.setHealth(wolf.getMaxHealth());
        }
        statsApplied = true;
    }

    private static void applyModifier(Wolf wolf, Holder<net.minecraft.world.entity.ai.attributes.Attribute> attr,
                                       ResourceLocation modId, double amount)
    {
        AttributeInstance inst = wolf.getAttribute(attr);
        if (inst == null) return;
        inst.removeModifier(modId);
        if (amount > 0.0001) {
            AttributeModifier mod = new AttributeModifier(modId, amount, AttributeModifier.Operation.ADD_VALUE);
            inst.addOrReplacePermanentModifier(mod);
        }
    }

    private static double cfg(ForgeConfigSpec.DoubleValue v, double fallback)
    {
        try {
            return v != null && v.get() != null ? v.get() : fallback;
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider)
    {
        CompoundTag tag = new CompoundTag();
        tag.putInt("level", level);
        tag.putInt("xp", xp);
        tag.putBoolean("stats_applied", statsApplied);
        tag.putString("behavior", behavior.name());
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt)
    {
        if (nbt == null) return;
        this.level = Math.max(1, nbt.getInt("level"));
        this.xp = Math.max(0, nbt.getInt("xp"));
        this.statsApplied = nbt.getBoolean("stats_applied");
        try {
            this.behavior = DogBehavior.valueOf(nbt.getString("behavior"));
        } catch (Throwable ignored) {
            this.behavior = DogBehavior.DEFAULT;
        }
        if (this.level > MAX_LEVEL) this.level = MAX_LEVEL;
    }
}

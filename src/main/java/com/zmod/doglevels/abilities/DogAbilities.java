package com.zmod.doglevels.abilities;

import com.zmod.doglevels.DogLevelsMod;
import com.zmod.doglevels.capability.DogLevelData;
import com.zmod.doglevels.config.DogLevelsConfig;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Combat abilities unlocked at configurable milestone levels.
 *
 * CHANGES IN 1.4.0:
 *   - Milestone levels are read live from config (was hardcoded constants).
 *   - Splash Bite now uses the {@link Enemy} interface instead of {@code Monster},
 *     so it also hits Slimes, MagmaCubes, and other hostile mobs that don't extend Monster.
 *   - Splash Bite radius, damage multiplier, Pack Hunter period/radius/buff duration,
 *     Howl of Vitality chance/heal, Lifesteal %, and effect refresh durations are all
 *     configurable now.
 */
public final class DogAbilities
{
    private DogAbilities() {}

    public static void bootstrap()
    {
        DogLevelsMod.LOGGER.info("Dog abilities registered (configurable milestone levels + tuning)");
    }

    // ---------- milestone level getters (live from config) ----------

    public static int splashBiteLevel()       { return DogLevelsConfig.getInt(DogLevelsConfig.SPLASH_BITE_LEVEL, 5); }
    public static int fireResistLevel()       { return DogLevelsConfig.getInt(DogLevelsConfig.FIRE_RESIST_LEVEL, 10); }
    public static int packHunterLevel()       { return DogLevelsConfig.getInt(DogLevelsConfig.PACK_HUNTER_LEVEL, 15); }
    public static int howlOfVitalityLevel()   { return DogLevelsConfig.getInt(DogLevelsConfig.HOWL_OF_VITALITY_LEVEL, 20); }
    public static int lifestealLevel()        { return DogLevelsConfig.getInt(DogLevelsConfig.LIFESTEAL_LEVEL, 25); }
    public static int bondedEnduranceLevel()  { return DogLevelsConfig.getInt(DogLevelsConfig.BONDED_ENDURANCE_LEVEL, 30); }

    // ---------- unlock checks ----------

    public static boolean hasSplashBite(DogLevelData data) {
        return data != null && data.getLevel() >= splashBiteLevel()
                && DogLevelsConfig.getBool(DogLevelsConfig.ENABLE_SPLASH_BITE, true);
    }

    public static boolean hasFireResistance(DogLevelData data) {
        return data != null && data.getLevel() >= fireResistLevel()
                && DogLevelsConfig.getBool(DogLevelsConfig.ENABLE_FIRE_RESIST, true);
    }

    public static boolean hasPackHunter(DogLevelData data) {
        return data != null && data.getLevel() >= packHunterLevel()
                && DogLevelsConfig.getBool(DogLevelsConfig.ENABLE_PACK_HUNTER, true);
    }

    public static boolean hasHowlOfVitality(DogLevelData data) {
        return data != null && data.getLevel() >= howlOfVitalityLevel()
                && DogLevelsConfig.getBool(DogLevelsConfig.ENABLE_HOWL_OF_VITALITY, true);
    }

    public static boolean hasLifesteal(DogLevelData data) {
        return data != null && data.getLevel() >= lifestealLevel()
                && DogLevelsConfig.getBool(DogLevelsConfig.ENABLE_LIFESTEAL, true);
    }

    public static boolean hasBondedEndurance(DogLevelData data) {
        return data != null && data.getLevel() >= bondedEnduranceLevel()
                && DogLevelsConfig.getBool(DogLevelsConfig.ENABLE_BONDED_ENDURANCE, true);
    }

    // ---------- ability implementations ----------

    /**
     * Splash Bite: when a high-level dog attacks a target, it also damages other
     * nearby HOSTILE mobs within a small radius.
     *
     * Uses the {@link Enemy} interface so it covers Slimes, MagmaCubes, etc. that
     * don't extend {@code Monster}. Does NOT hit passive animals, other wolves,
     * or players.
     */
    public static void doSplashBite(Wolf dog, LivingEntity primaryTarget, float damage)
    {
        if (dog.level().isClientSide) return;
        double radius = DogLevelsConfig.getDouble(DogLevelsConfig.SPLASH_BITE_RADIUS, 2.5);
        float mult = (float) DogLevelsConfig.getDouble(DogLevelsConfig.SPLASH_BITE_DAMAGE_MULT, 0.5);
        AABB box = dog.getBoundingBox().inflate(radius);
        List<LivingEntity> others = dog.level().getEntitiesOfClass(LivingEntity.class, box, e ->
                e != primaryTarget && e != dog
                        && e.isAlive()
                        && e.isAttackable()
                        && e instanceof Enemy
                        && !(e instanceof Player));
        for (LivingEntity e : others) {
            e.hurt(dog.damageSources().mobAttack(dog), damage * mult);
        }
    }

    public static void doPackHunterHowl(Wolf dog)
    {
        if (dog.level().isClientSide) return;
        double radius = DogLevelsConfig.getDouble(DogLevelsConfig.PACK_HUNTER_RADIUS, 8.0);
        int buffTicks = DogLevelsConfig.getInt(DogLevelsConfig.PACK_HUNTER_BUFF_TICKS, 120);
        AABB box = dog.getBoundingBox().inflate(radius);
        List<LivingEntity> allies = dog.level().getEntitiesOfClass(LivingEntity.class, box, e ->
                (e instanceof Wolf w && w.isTame()) || e == dog);
        for (LivingEntity ally : allies) {
            ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, buffTicks, 0, true, true));
        }
        LivingEntity owner = dog.getOwner();
        if (owner instanceof Player p) {
            p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, buffTicks, 0, true, true));
        }
    }

    public static void doHowlOfVitality(Wolf dog)
    {
        if (dog.level().isClientSide) return;
        float heal = (float) DogLevelsConfig.getDouble(DogLevelsConfig.HOWL_OF_VITALITY_HEAL, 1.0);
        dog.heal(heal);
        LivingEntity owner = dog.getOwner();
        if (owner instanceof Player p && p.getHealth() < p.getMaxHealth()) {
            p.heal(heal);
        }
    }

    public static void applyBondedEndurance(Wolf dog)
    {
        if (dog.level().isClientSide) return;
        int ticks = DogLevelsConfig.getInt(DogLevelsConfig.BONDED_ENDURANCE_REFRESH_TICKS, 200);
        if (dog.getEffect(MobEffects.REGENERATION) == null) {
            dog.addEffect(new MobEffectInstance(MobEffects.REGENERATION, ticks, 1, true, false));
        }
    }

    public static void applyFireResistance(Wolf dog)
    {
        if (dog.level().isClientSide) return;
        int ticks = DogLevelsConfig.getInt(DogLevelsConfig.FIRE_RESIST_REFRESH_TICKS, 400);
        if (dog.getEffect(MobEffects.FIRE_RESISTANCE) == null) {
            dog.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, ticks, 0, true, false));
        }
    }

    public static float doLifesteal(Wolf dog, float damageDealt)
    {
        if (dog.level().isClientSide) return 0F;
        float pct = (float) DogLevelsConfig.getDouble(DogLevelsConfig.LIFESTEAL_PERCENT, 0.25);
        float heal = damageDealt * pct;
        dog.heal(heal);
        return heal;
    }
}

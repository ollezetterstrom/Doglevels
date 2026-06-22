package com.zmod.doglevels.abilities;

import com.zmod.doglevels.DogLevelsMod;
import com.zmod.doglevels.capability.DogLevelData;
import com.zmod.doglevels.config.DogLevelsConfig;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.List;

public final class DogAbilities
{
    public static final int SPLASH_BITE_LEVEL = 5;
    public static final int FIRE_RESIST_LEVEL = 10;
    public static final int PACK_HUNTER_LEVEL = 15;
    public static final int HOWL_OF_VITALITY_LEVEL = 20;
    public static final int LIFESTEAL_LEVEL = 25;
    public static final int BONDED_ENDURANCE_LEVEL = 30;

    private DogAbilities() {}

    public static void bootstrap()
    {
        DogLevelsMod.LOGGER.info("Dog abilities registered");
    }

    public static boolean hasSplashBite(DogLevelData data)
    {
        return data != null && data.getLevel() >= SPLASH_BITE_LEVEL
                && DogLevelsConfig.ENABLE_SPLASH_BITE.get();
    }

    public static boolean hasFireResistance(DogLevelData data)
    {
        return data != null && data.getLevel() >= FIRE_RESIST_LEVEL
                && DogLevelsConfig.ENABLE_FIRE_RESIST.get();
    }

    public static boolean hasPackHunter(DogLevelData data)
    {
        return data != null && data.getLevel() >= PACK_HUNTER_LEVEL
                && DogLevelsConfig.ENABLE_PACK_HUNTER.get();
    }

    public static boolean hasHowlOfVitality(DogLevelData data)
    {
        return data != null && data.getLevel() >= HOWL_OF_VITALITY_LEVEL
                && DogLevelsConfig.ENABLE_HOWL_OF_VITALITY.get();
    }

    public static boolean hasLifesteal(DogLevelData data)
    {
        return data != null && data.getLevel() >= LIFESTEAL_LEVEL
                && DogLevelsConfig.ENABLE_LIFESTEAL.get();
    }

    public static boolean hasBondedEndurance(DogLevelData data)
    {
        return data != null && data.getLevel() >= BONDED_ENDURANCE_LEVEL
                && DogLevelsConfig.ENABLE_BONDED_ENDURANCE.get();
    }

    /**
     * Splash Bite: when a high-level dog attacks a target, it also damages other
     * nearby HOSTILE mobs within a small radius (50% damage).
     *
     * Only hits entities that implement Monster (zombies, skeletons, creepers, etc).
     * Does NOT hit passive animals (sheep, cows, etc.) or other wolves or players.
     */
    public static void doSplashBite(Wolf dog, LivingEntity primaryTarget, float damage)
    {
        if (dog.level().isClientSide) return;
        AABB box = dog.getBoundingBox().inflate(2.5D);
        List<LivingEntity> others = dog.level().getEntitiesOfClass(LivingEntity.class, box, e ->
                e != primaryTarget && e != dog
                        && e.isAlive()
                        && e instanceof net.minecraft.world.entity.monster.Monster
                        && !(e instanceof Player));
        for (LivingEntity e : others) {
            e.hurt(dog.damageSources().mobAttack(dog), damage * 0.5F);
        }
    }

    public static void doPackHunterHowl(Wolf dog)
    {
        if (dog.level().isClientSide) return;
        AABB box = dog.getBoundingBox().inflate(8.0D);
        List<LivingEntity> allies = dog.level().getEntitiesOfClass(LivingEntity.class, box, e ->
                (e instanceof Wolf w && w.isTame()) || e == dog);
        for (LivingEntity ally : allies) {
            ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 120, 0, true, true));
        }
        LivingEntity owner = dog.getOwner();
        if (owner instanceof Player p) {
            p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 120, 0, true, true));
        }
    }

    public static void doHowlOfVitality(Wolf dog)
    {
        if (dog.level().isClientSide) return;
        dog.heal(1.0F);
        LivingEntity owner = dog.getOwner();
        if (owner instanceof Player p && p.getHealth() < p.getMaxHealth()) {
            p.heal(1.0F);
        }
    }

    public static void applyBondedEndurance(Wolf dog)
    {
        if (dog.level().isClientSide) return;
        if (dog.getEffect(MobEffects.REGENERATION) == null) {
            dog.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1, true, false));
        }
    }

    public static void applyFireResistance(Wolf dog)
    {
        if (dog.level().isClientSide) return;
        if (dog.getEffect(MobEffects.FIRE_RESISTANCE) == null) {
            dog.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 400, 0, true, false));
        }
    }

    public static float doLifesteal(Wolf dog, float damageDealt)
    {
        if (dog.level().isClientSide) return 0F;
        float heal = damageDealt * 0.25F;
        dog.heal(heal);
        return heal;
    }
}

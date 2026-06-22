package com.zmod.doglevels.events;

import com.zmod.doglevels.DogLevelsMod;
import com.zmod.doglevels.abilities.DogAbilities;
import com.zmod.doglevels.capability.CapabilityHelper;
import com.zmod.doglevels.capability.DogLevelCapabilities;
import com.zmod.doglevels.capability.DogLevelData;
import com.zmod.doglevels.config.DogLevelsConfig;
import com.zmod.doglevels.network.DogLevelNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Handles XP gain from combat and ability triggers.
 *
 * XP is granted in two scenarios:
 *   1. Wolf lands the killing blow: full XP (mob max health × multiplier)
 *   2. Player lands the killing blow: all tamed wolves owned by the player
 *      within 32 blocks get ASSIST XP (50% of full, configurable via
 *      XP_FROM_PLAYER_KILL_MULT which defaults to 0.5)
 */
@Mod.EventBusSubscriber(modid = DogLevelsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DogLevelEvents
{
    public static void register() {}

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event)
    {
        if (event.getEntity().level().isClientSide) return;
        LivingEntity victim = event.getEntity();
        Object src = event.getSource().getEntity();

        if (src instanceof Wolf wolf && wolf.isTame()) {
            // Wolf got the kill — full XP
            grantXP(wolf, victim, 1.0);
        } else if (src instanceof Player player) {
            // Player got the kill — assist XP to all nearby tamed wolves
            grantAssistXP(player, victim);
        }
    }

    private static void grantXP(Wolf wolf, LivingEntity victim, double mult)
    {
        double baseMult = DogLevelsConfig.XP_FROM_KILL_HEALTH_MULT.get();
        int xp = (int) Math.max(1, Math.ceil(victim.getMaxHealth() * baseMult * mult));

        CapabilityHelper.getCapability(wolf, DogLevelCapabilities.DOG_LEVEL).ifPresent(data -> {
            int ups = data.addXP(xp);
            if (ups > 0) {
                data.applyStats(wolf);
                notifyLevelUp(wolf, data.getLevel());
                DogLevelNetwork.sendDogLevelUpdate(wolf);
            } else if (!data.isMaxLevel()) {
                DogLevelNetwork.sendDogLevelUpdate(wolf);
            }
        });
    }

    private static void grantAssistXP(Player player, LivingEntity victim)
    {
        double assistMult = cfg(DogLevelsConfig.XP_FROM_PLAYER_KILL_MULT, 0.5);
        if (assistMult <= 0) return;

        AABB box = player.getBoundingBox().inflate(32.0D);
        List<Wolf> wolves = player.level().getEntitiesOfClass(Wolf.class, box,
                w -> w.isTame() && w.getOwner() == player);

        for (Wolf wolf : wolves) {
            grantXP(wolf, victim, assistMult);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingHurt(LivingHurtEvent event)
    {
        if (event.getEntity().level().isClientSide) return;

        Object src = event.getSource().getEntity();
        if (src instanceof Wolf wolf && wolf.isTame()) {
            CapabilityHelper.getCapability(wolf, DogLevelCapabilities.DOG_LEVEL).ifPresent(data -> {
                float dmg = event.getAmount();
                if (DogAbilities.hasSplashBite(data)) {
                    DogAbilities.doSplashBite(wolf, event.getEntity(), dmg);
                }
                if (DogAbilities.hasLifesteal(data)) {
                    DogAbilities.doLifesteal(wolf, dmg);
                }
                if (DogAbilities.hasHowlOfVitality(data) && wolf.getRandom().nextFloat() < 0.2F) {
                    DogAbilities.doHowlOfVitality(wolf);
                }
            });
        }
    }

    private static void notifyLevelUp(Wolf wolf, int newLevel)
    {
        LivingEntity owner = wolf.getOwner();
        if (owner instanceof Player p) {
            p.sendSystemMessage(Component.literal(
                    "Your dog reached level " + newLevel + "!"));
        }
    }

    private static double cfg(net.minecraftforge.common.ForgeConfigSpec.DoubleValue v, double fallback)
    {
        try {
            return v != null && v.get() != null ? v.get() : fallback;
        } catch (Throwable ignored) {
            return fallback;
        }
    }
}

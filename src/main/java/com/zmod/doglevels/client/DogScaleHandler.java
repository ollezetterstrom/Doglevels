package com.zmod.doglevels.client;

import com.zmod.doglevels.DogLevelsMod;
import com.zmod.doglevels.capability.DogLevelData;
import com.zmod.doglevels.config.DogLevelsConfig;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Scales the wolf render based on its level.
 *
 * Reads the MAX_HEALTH AttributeModifier (added by DogLevelData.applyStats)
 * to determine the level, then applies a scale to the PoseStack.
 *
 * IMPORTANT: We only push in Pre and only pop in Post if we actually pushed.
 * Since RenderLivingEvent.Post always fires after Pre (unless the Pre is
 * canceled, which we don't do), the push/pop are always balanced.
 */
@Mod.EventBusSubscriber(modid = DogLevelsMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DogScaleHandler
{
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event)
    {
        if (!(event.getEntity() instanceof Wolf wolf)) return;
        if (!wolf.level().isClientSide) return;

        float scale = computeScale(wolf);
        if (scale != 1.0F) {
            event.getPoseStack().pushPose();
            event.getPoseStack().scale(scale, scale, scale);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event)
    {
        if (!(event.getEntity() instanceof Wolf wolf)) return;
        if (!wolf.level().isClientSide) return;

        float scale = computeScale(wolf);
        if (scale != 1.0F) {
            event.getPoseStack().popPose();
        }
    }

    private static float computeScale(Wolf wolf)
    {
        int level = computeLevelFromModifier(wolf);
        if (level <= 1) return 1.0F;
        double perLevel = cfg(DogLevelsConfig.SIZE_PER_LEVEL, 0.015);
        double maxBonus = cfg(DogLevelsConfig.MAX_SIZE_BONUS, 0.6);
        double bonus = Math.min(maxBonus, perLevel * (level - 1));
        return (float) (1.0 + bonus);
    }

    private static int computeLevelFromModifier(Wolf wolf)
    {
        AttributeInstance healthAttr = wolf.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr == null) return 1;
        AttributeModifier mod = healthAttr.getModifier(DogLevelData.HEALTH_MOD_ID);
        if (mod == null) return 1;
        double healthPerLevel = cfg(DogLevelsConfig.HEALTH_PER_LEVEL, 1.0);
        if (healthPerLevel <= 0) return 1;
        return 1 + (int) Math.round(mod.amount() / healthPerLevel);
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

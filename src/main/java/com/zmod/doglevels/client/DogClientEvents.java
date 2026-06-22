package com.zmod.doglevels.client;

import com.zmod.doglevels.DogLevelsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-only event handler that opens the {@link DogScreen} when the player
 * shift+right-clicks a tamed wolf with an empty hand.
 *
 * CRITICAL: Must run at HIGHEST priority, BEFORE the common handler
 * ({@link com.zmod.doglevels.events.DogInteractionEvents}) which runs at HIGH
 * priority and cancels the event. If this runs at a lower priority, the event
 * is already canceled and this handler never fires (default @SubscribeEvent
 * has receiveCanceled=false).
 *
 * Event flow on client:
 *   1. HIGHEST (this handler) → opens Screen
 *   2. HIGH (common handler) → cancels event (prevents vanilla sit/stand toggle)
 *
 * Event flow on server:
 *   1. HIGH (common handler) → cancels event (prevents vanilla sit/stand toggle)
 */
@Mod.EventBusSubscriber(modid = DogLevelsMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DogClientEvents
{
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityInteractClient(PlayerInteractEvent.EntityInteract event)
    {
        if (!event.getEntity().level().isClientSide) return;
        if (!(event.getTarget() instanceof Wolf wolf)) return;
        if (!wolf.isTame()) return;
        if (!event.getItemStack().isEmpty()) return;
        if (!event.getEntity().isShiftKeyDown()) return;

        // Open the screen
        Minecraft.getInstance().setScreen(new DogScreen(wolf.getId()));
    }
}

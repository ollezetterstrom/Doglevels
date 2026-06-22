package com.zmod.doglevels.events;

import com.zmod.doglevels.DogLevelsMod;
import com.zmod.doglevels.abilities.DogAbilities;
import com.zmod.doglevels.capability.CapabilityHelper;
import com.zmod.doglevels.capability.DogLevelCapabilities;
import com.zmod.doglevels.capability.DogLevelData;
import com.zmod.doglevels.config.DogLevelsConfig;
import com.zmod.doglevels.items.ModItems;
import com.zmod.doglevels.network.DogLevelNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles right-click interactions with tamed wolves:
 *   - Treat item: feeds the dog for instant XP (cancels the event)
 *   - Empty hand + sneak: opens the DogScreen UI (client-side; cancels event on both sides)
 *   - Empty hand (no sneak): brief chat summary (does NOT cancel — vanilla sit/stand toggles)
 */
@Mod.EventBusSubscriber(modid = DogLevelsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DogInteractionEvents
{
    public static void register() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event)
    {
        if (!(event.getTarget() instanceof Wolf wolf)) return;
        if (!wolf.isTame()) return;

        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();

        // Treat feeding — only on server
        if (stack.getItem() instanceof com.zmod.doglevels.items.TreatItem) {
            if (player.level().isClientSide) return;
            event.setCanceled(true);
            CapabilityHelper.getCapability(wolf, DogLevelCapabilities.DOG_LEVEL).ifPresent(data -> {
                int xp = DogLevelsConfig.XP_FROM_TREAT.get();
                int ups = data.addXP(xp);
                if (ups > 0) {
                    data.applyStats(wolf);
                    player.sendSystemMessage(Component.literal(
                            "Your dog loved the treat and reached level " + data.getLevel() + "!"));
                } else if (data.isMaxLevel()) {
                    player.sendSystemMessage(Component.literal(
                            "Your dog is already at max level!").withStyle(ChatFormatting.GOLD));
                } else {
                    int need = data.xpToNextLevel();
                    int cur = data.getXP();
                    player.sendSystemMessage(Component.literal(
                            "Dog gained " + xp + " XP (" + cur + "/" + need + ")."));
                }
                DogLevelNetwork.sendDogLevelUpdate(wolf);
            });
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            wolf.level().broadcastEntityEvent(wolf, (byte) 7);
            return;
        }

        // Empty hand + sneak → open Screen (client) / cancel (server)
        if (stack.isEmpty() && player.isShiftKeyDown()) {
            event.setCanceled(true);
            // Client-side Screen opening is handled by DogClientEvents
            // Server-side: just cancel to prevent sit/stand toggle
            return;
        }

        // Empty hand (no sneak) → brief summary (don't cancel — vanilla sit/stand toggles)
        if (stack.isEmpty() && !player.level().isClientSide) {
            CapabilityHelper.getCapability(wolf, DogLevelCapabilities.DOG_LEVEL).ifPresent(data -> {
                var msg = Component.literal("Dog ")
                        .append(Component.literal("[Lv " + data.getLevel() + "]").withStyle(ChatFormatting.AQUA));
                if (!data.isMaxLevel()) {
                    msg.append(" XP " + data.getXP() + "/" + data.xpToNextLevel());
                } else {
                    msg.append(" MAX").withStyle(ChatFormatting.GOLD);
                }
                msg.append(" [" + data.getBehavior().name() + "]");
                player.sendSystemMessage(msg);
            });
        }
    }
}

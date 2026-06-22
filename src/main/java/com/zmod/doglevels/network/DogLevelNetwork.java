package com.zmod.doglevels.network;

import com.zmod.doglevels.DogLevelsMod;
import com.zmod.doglevels.capability.CapabilityHelper;
import com.zmod.doglevels.capability.DogLevelCapabilities;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

/**
 * Real networking implementation (1.4.0+).
 *
 * Sends {@link DogLevelSyncMessage} server → client whenever a dog's level, XP,
 * or behavior changes, and also when a player starts tracking a dog (so freshly
 * logged-in players see the correct state immediately).
 *
 * Replaces the 1.3.2 no-op stub that relied on the client inferring the level
 * from the synced MAX_HEALTH attribute. That worked but had three bugs:
 *   1. The GUI's behavior cycle button used a static prediction shared across
 *      all dogs (fix: server is now authoritative; GUI reads the synced value).
 *   2. The GUI couldn't show an XP bar (fix: now has xp + xpToNext).
 *   3. The render scale handler miscomputed the level if the server owner
 *      changed health_per_level mid-game (fix: scale handler reads the cache).
 */
public final class DogLevelNetwork
{
    private static final int PROTOCOL_VERSION = 1;

    public static final SimpleChannel CHANNEL = ChannelBuilder.named(
            com.zmod.doglevels.DogLevelsMod.MOD_ID + ":main")
            .networkProtocolVersion(PROTOCOL_VERSION)
            .optional()
            .simpleChannel();

    private static boolean registered = false;

    private DogLevelNetwork() {}

    public static void register()
    {
        if (registered) return;
        registered = true;

        CHANNEL.messageBuilder(DogLevelSyncMessage.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(DogLevelSyncMessage::encode)
                .decoder(DogLevelSyncMessage::decode)
                .consumerMainThread(DogLevelSyncMessage::handle)
                .add();

        DogLevelsMod.LOGGER.info("Dog Levels network channel registered");
    }

    /**
     * Send a sync packet for the given wolf to all players tracking it (and the
     * wolf itself if it's owned by a player, which it always is for tamed wolves).
     * Safe to call on either side; on the client it's a no-op.
     */
    public static void sendDogLevelUpdate(Wolf wolf)
    {
        if (wolf == null || wolf.level().isClientSide) return;
        CapabilityHelper.getCapability(wolf, DogLevelCapabilities.DOG_LEVEL).ifPresent(data -> {
            DogLevelSyncMessage msg = DogLevelSyncMessage.from(wolf, data);
            CHANNEL.send(msg, PacketDistributor.TRACKING_ENTITY_AND_SELF.with(wolf));
        });
    }

    /**
     * Send a sync packet for the given wolf to a specific player (used by the
     * PlayerEvent.StartTracking hook so freshly logged-in players see the
     * correct state immediately).
     */
    public static void sendDogLevelUpdateTo(Wolf wolf, ServerPlayer player)
    {
        if (wolf == null || player == null) return;
        if (wolf.level().isClientSide) return;
        CapabilityHelper.getCapability(wolf, DogLevelCapabilities.DOG_LEVEL).ifPresent(data -> {
            DogLevelSyncMessage msg = DogLevelSyncMessage.from(wolf, data);
            CHANNEL.send(msg, PacketDistributor.PLAYER.with(player));
        });
    }
}

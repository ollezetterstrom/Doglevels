package com.zmod.doglevels.network;

import com.zmod.doglevels.capability.DogBehavior;
import com.zmod.doglevels.capability.DogLevelData;
import com.zmod.doglevels.client.ClientDogLevelCache;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraftforge.event.network.CustomPayloadEvent;

/**
 * Server → client sync message for a single dog's level/XP/behavior.
 *
 * Replaces the no-op networking stub from 1.3.2. The client stores received
 * data in {@link ClientDogLevelCache} keyed by entity id, so the GUI and the
 * render scale handler can read the actual level without inferring it from
 * the synced MAX_HEALTH attribute.
 *
 * Packet lifecycle:
 *   - Sent on level-up, XP gain, behavior change, treat feed.
 *   - Sent when a player starts tracking the wolf (PlayerEvent.StartTracking).
 *   - Sent on entity join (so newly logged-in players see correct state).
 *
 * Registered in {@link DogLevelNetwork#register(DogLevelSyncMessage, BiConsumer, ...)}
 * via SimpleChannel#messageBuilder().encoder().decoder().consumerMainThread().
 */
public record DogLevelSyncMessage(
        int entityId,
        int level,
        int xp,
        int xpToNext,
        int maxLevel,
        int behaviorOrdinal
) {
    public static void encode(DogLevelSyncMessage msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeVarInt(msg.level);
        buf.writeVarInt(msg.xp);
        buf.writeVarInt(msg.xpToNext);
        buf.writeVarInt(msg.maxLevel);
        buf.writeVarInt(msg.behaviorOrdinal);
    }

    public static DogLevelSyncMessage decode(FriendlyByteBuf buf) {
        return new DogLevelSyncMessage(
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt()
        );
    }

    public static DogLevelSyncMessage from(Wolf wolf, DogLevelData data) {
        return new DogLevelSyncMessage(
                wolf.getId(),
                data.getLevel(),
                data.getXP(),
                data.xpToNextLevel(),
                DogLevelData.getMaxLevel(),
                data.getBehavior().ordinal()
        );
    }

    /** Main-thread handler (registered via consumerMainThread in DogLevelNetwork). */
    public static void handle(DogLevelSyncMessage msg, CustomPayloadEvent.Context ctx) {
        ClientDogLevelCache.update(
                msg.entityId, msg.level, msg.xp, msg.xpToNext, msg.maxLevel, msg.behaviorOrdinal);
        ctx.setPacketHandled(true);
    }

    /** Translate the behavior ordinal back to the enum (used by the client cache + tests). */
    public static DogBehavior behaviorFromOrdinal(int ordinal) {
        DogBehavior[] values = DogBehavior.values();
        if (ordinal < 0 || ordinal >= values.length) return DogBehavior.DEFAULT;
        return values[ordinal];
    }
}

package com.zmod.doglevels;

import com.zmod.doglevels.capability.DogBehavior;
import com.zmod.doglevels.network.DogLevelSyncMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for {@link DogLevelSyncMessage} encode/decode round-trip and behavior
 * ordinal mapping. These run without a Minecraft server because the message
 * uses {@link net.minecraft.network.FriendlyByteBuf} directly.
 */
class DogLevelSyncMessageTest {

    @Test
    void encodeDecode_roundTripsAllFields() {
        DogLevelSyncMessage original = new DogLevelSyncMessage(
                42,         // entityId
                17,         // level
                13,         // xp
                30,         // xpToNext
                30,         // maxLevel
                DogBehavior.AGGRESSIVE.ordinal()  // behaviorOrdinal
        );

        net.minecraft.network.FriendlyByteBuf buf =
                new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        DogLevelSyncMessage.encode(original, buf);

        DogLevelSyncMessage decoded = DogLevelSyncMessage.decode(buf);

        assertEquals(original.entityId(),       decoded.entityId());
        assertEquals(original.level(),          decoded.level());
        assertEquals(original.xp(),             decoded.xp());
        assertEquals(original.xpToNext(),       decoded.xpToNext());
        assertEquals(original.maxLevel(),       decoded.maxLevel());
        assertEquals(original.behaviorOrdinal(), decoded.behaviorOrdinal());
    }

    @Test
    void behaviorFromOrdinal_returnsCorrectEnum() {
        assertEquals(DogBehavior.DEFAULT,    DogLevelSyncMessage.behaviorFromOrdinal(0));
        assertEquals(DogBehavior.AGGRESSIVE, DogLevelSyncMessage.behaviorFromOrdinal(1));
        assertEquals(DogBehavior.PASSIVE,    DogLevelSyncMessage.behaviorFromOrdinal(2));
    }

    @Test
    void behaviorFromOrdinal_outOfRange_returnsDefault() {
        assertEquals(DogBehavior.DEFAULT, DogLevelSyncMessage.behaviorFromOrdinal(-1));
        assertEquals(DogBehavior.DEFAULT, DogLevelSyncMessage.behaviorFromOrdinal(99));
    }
}

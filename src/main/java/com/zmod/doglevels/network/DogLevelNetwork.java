package com.zmod.doglevels.network;

/**
 * Network placeholder.
 *
 * The Forge 1.21.1 networking API has changed significantly: SimpleChannel now
 * uses SimpleConnection.play() with RegistryFriendlyByteBuf and a complex handler
 * protocol abstraction. To keep this mod simple and avoid runtime issues, we
 * DO NOT register any custom payloads. Instead:
 *
 *   - Level/XP data is stored server-side only (via the capability + NBT).
 *   - Stat changes are applied to the Wolf's attributes server-side, which
 *     are automatically synced to clients by vanilla via the attribute
 *     sync packets.
 *   - Size scaling is computed on the client by reading the wolf's max health
 *     attribute (which IS synced), so the rendering scales correctly without
 *     any custom packet.
 *   - Stat sheet display is sent via the player's chat (server-side message).
 *
 * If you want to extend this with a custom GUI, see:
 * https://docs.minecraftforge.net/en/1.21.x/networking/
 */
public final class DogLevelNetwork
{
    private DogLevelNetwork() {}

    public static void register()
    {
        // No custom packets needed - see class javadoc.
    }

    /**
     * Server-side stub: would normally send a sync packet.
     * Currently a no-op because attribute changes are auto-synced by vanilla.
     */
    public static void sendDogLevelUpdate(net.minecraft.world.entity.animal.Wolf wolf)
    {
        // No-op: attribute changes propagate automatically.
    }
}

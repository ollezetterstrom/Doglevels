package com.zmod.doglevels.capability;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;

/**
 * Holds the singleton Capability reference for {@link DogLevelData}.
 *
 * Forge 1.21.1 still uses the {@code CapabilityToken} pattern: calling
 * {@link CapabilityManager#get(CapabilityToken)} returns a non-null singleton
 * that is shared by everyone (the token is anonymous, so the generic type is
 * captured reflectively).
 */
public final class DogLevelCapabilities
{
    public static final Capability<DogLevelData> DOG_LEVEL =
            CapabilityManager.get(new CapabilityToken<>() {});

    private DogLevelCapabilities() {}

    public static void register(final RegisterCapabilitiesEvent event)
    {
        event.register(DogLevelData.class);
    }
}

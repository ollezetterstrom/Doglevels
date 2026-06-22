package com.zmod.doglevels.capability;

import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;

/**
 * Compile-time bridge to access Forge capabilities on Entity.
 *
 * In Forge 1.21.1, Entity extends CapabilityProvider<Entity> at runtime (via Forge patches),
 * but the vanilla deobf MC jar we compile against doesn't have these patches applied.
 * So Entity doesn't have a getCapability method at compile time.
 *
 * At runtime, however, Entity DOES implement ICapabilityProvider (through CapabilityProvider).
 * So we can cast and call getCapability on the ICapabilityProvider interface.
 *
 * This helper centralizes the cast so the rest of the code can use a clean syntax.
 */
public final class CapabilityHelper
{
    private CapabilityHelper() {}

    public static <T> LazyOptional<T> getCapability(Entity entity, Capability<T> cap)
    {
        if (entity instanceof ICapabilityProvider provider) {
            return provider.getCapability(cap);
        }
        return LazyOptional.empty();
    }
}

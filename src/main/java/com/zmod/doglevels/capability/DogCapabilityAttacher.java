package com.zmod.doglevels.capability;

import com.zmod.doglevels.DogLevelsMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Attaches the {@link DogLevelData} capability to wolves when they enter the world.
 *
 * In Forge 1.21.1, Entity extends CapabilityProvider<Entity> at runtime (via Forge patches),
 * so it implements {@link ICapabilityProvider}. The deobf MC jar we compile against doesn't
 * have these patches applied, so we go through the {@link ICapabilityProvider} interface
 * explicitly via {@link CapabilityHelper#getCapability(Entity, Capability)}.
 */
@Mod.EventBusSubscriber(modid = DogLevelsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DogCapabilityAttacher
{
    public static final ResourceLocation DOG_LEVEL_ID =
            ResourceLocation.fromNamespaceAndPath(DogLevelsMod.MOD_ID, "dog_level");

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event)
    {
        if (event.getObject() instanceof Wolf) {
            if (CapabilityHelper.getCapability(event.getObject(), DogLevelCapabilities.DOG_LEVEL).isPresent()) return;
            event.addCapability(DOG_LEVEL_ID, new DogLevelProvider());
        }
    }
}

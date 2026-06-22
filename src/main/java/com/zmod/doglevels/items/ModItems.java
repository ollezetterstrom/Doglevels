package com.zmod.doglevels.items;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Deferred register for all items in the Dog Levels mod.
 *
 * IMPORTANT: All RegistryObject constants MUST live here (in a class that is
 * force-loaded during mod construction via {@code ModItems.ITEMS.register(modEventBus)}),
 * not in the item class itself. Otherwise, the item class is only loaded lazily
 * (e.g. when {@code addCreative} runs to build the creative tab), which is *after*
 * the {@code RegisterEvent} has fired — and DeferredRegister throws
 * "Cannot register new entries to DeferredRegister after RegisterEvent has been fired."
 */
public final class ModItems
{
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, "doglevels");

    public static final RegistryObject<TreatItem> TREAT_ITEM =
            ITEMS.register("treat",
                    () -> new TreatItem(new Item.Properties().stacksTo(64)));
}

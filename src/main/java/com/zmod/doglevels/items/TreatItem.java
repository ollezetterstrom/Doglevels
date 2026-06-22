package com.zmod.doglevels.items;

import net.minecraft.world.item.Item;

/**
 * Item: Treat. Right-click on a tamed wolf to feed it for instant XP
 * (see {@link com.zmod.doglevels.events.DogInteractionEvents}).
 *
 * The RegistryObject for this item lives in {@link ModItems#TREAT_ITEM} —
 * see that class's javadoc for why RegistryObjects must NOT be declared
 * on the item class itself.
 */
public class TreatItem extends Item
{
    public TreatItem(Properties props)
    {
        super(props);
    }
}

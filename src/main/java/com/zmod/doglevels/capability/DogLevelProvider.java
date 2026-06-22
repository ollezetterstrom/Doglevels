package com.zmod.doglevels.capability;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Attaches {@link DogLevelData} to a Wolf entity and persists it to NBT.
 *
 * 1.21.1: ICapabilitySerializable extends INBTSerializable, which now requires
 * a HolderLookup.Provider parameter on serialize/deserialize.
 */
public class DogLevelProvider implements ICapabilitySerializable<CompoundTag>
{
    private DogLevelData data;
    private final LazyOptional<DogLevelData> lazy;

    public DogLevelProvider()
    {
        this.data = new DogLevelData();
        this.lazy = LazyOptional.of(() -> data);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side)
    {
        if (cap == DogLevelCapabilities.DOG_LEVEL) {
            return lazy.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider)
    {
        return data.serializeNBT(provider);
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt)
    {
        data.deserializeNBT(provider, nbt);
    }

    public void invalidate()
    {
        lazy.invalidate();
    }
}

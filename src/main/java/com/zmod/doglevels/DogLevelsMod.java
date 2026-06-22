package com.zmod.doglevels;

import com.mojang.logging.LogUtils;
import com.zmod.doglevels.abilities.DogAbilities;
import com.zmod.doglevels.capability.DogLevelData;
import com.zmod.doglevels.config.DogLevelsConfig;
import com.zmod.doglevels.events.DogInteractionEvents;
import com.zmod.doglevels.events.DogLevelEvents;
import com.zmod.doglevels.events.DogTickEvents;
import com.zmod.doglevels.items.ModItems;
import com.zmod.doglevels.items.TreatItem;
import com.zmod.doglevels.network.DogLevelNetwork;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

@Mod(DogLevelsMod.MOD_ID)
public class DogLevelsMod
{
    public static final String MOD_ID = "doglevels";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    public static final RegistryObject<CreativeModeTab> DOG_LEVELS_TAB =
            CREATIVE_MODE_TABS.register("dog_levels_tab", () -> CreativeModeTab.builder(
                            net.minecraft.world.item.CreativeModeTab.Row.TOP, 0)
                    .icon(() -> new ItemStack(Items.BONE))
                    .title(Component.translatable("itemGroup.doglevels"))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.TREAT_ITEM.get());
                    })
                    .build());

    public DogLevelsMod(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();

        ModItems.ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        modEventBus.addListener(com.zmod.doglevels.capability.DogLevelCapabilities::register);
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addCreative);

        MinecraftForge.EVENT_BUS.register(this);
        DogLevelEvents.register();
        DogTickEvents.register();
        DogInteractionEvents.register();

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, DogLevelsConfig.SPEC, "doglevels-common.toml");
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        event.enqueueWork(() -> {
            DogLevelNetwork.register();
            DogAbilities.bootstrap();
            LOGGER.info("Dog Levels mod: common setup complete. Max level = {}", DogLevelData.MAX_LEVEL);
        });
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event)
    {
        if (event.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS)
        {
            event.accept(ModItems.TREAT_ITEM.get());
        }
    }
}

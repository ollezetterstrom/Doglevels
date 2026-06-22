package com.zmod.doglevels;

import com.mojang.logging.LogUtils;
import com.zmod.doglevels.abilities.DogAbilities;
import com.zmod.doglevels.capability.DogLevelData;
import com.zmod.doglevels.client.ClientDogLevelCache;
import com.zmod.doglevels.config.DogLevelsConfig;
import com.zmod.doglevels.events.DogInteractionEvents;
import com.zmod.doglevels.events.DogLevelEvents;
import com.zmod.doglevels.events.DogTickEvents;
import com.zmod.doglevels.items.ModItems;
import com.zmod.doglevels.network.DogLevelNetwork;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
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
        MinecraftForge.EVENT_BUS.addListener(DogLevelEvents::onLivingDeath);
        MinecraftForge.EVENT_BUS.addListener(DogLevelEvents::onLivingHurt);
        MinecraftForge.EVENT_BUS.addListener(DogLevelEvents::onStartTracking);
        DogLevelEvents.register();
        DogTickEvents.register();
        DogInteractionEvents.register();

        // Client-only: clear the dog level cache on disconnect so stale entries
        // from a previous world don't leak into the next one.
        MinecraftForge.EVENT_BUS.addListener(this::onClientDisconnect);

        // Server-side: clear session state when the server stops so a subsequent
        // world load re-adds TemptGoals and resets cooldowns.
        MinecraftForge.EVENT_BUS.addListener(this::onServerStopping);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, DogLevelsConfig.SPEC, "doglevels-common.toml");
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        event.enqueueWork(() -> {
            DogLevelNetwork.register();
            DogAbilities.bootstrap();
            LOGGER.info("Dog Levels mod: common setup complete. Max level = {}", DogLevelData.getMaxLevel());
        });
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event)
    {
        if (event.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS)
        {
            event.accept(ModItems.TREAT_ITEM.get());
        }
    }

    private void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event)
    {
        ClientDogLevelCache.clear();
    }

    private void onServerStopping(ServerStoppingEvent event)
    {
        DogTickEvents.clearSessionState();
    }
}

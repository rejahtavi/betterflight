package com.rejahtavi.betterflight;

import com.rejahtavi.betterflight.client.ClientConfig;
import com.rejahtavi.betterflight.client.gui.ClassicHudOverlay;
import com.rejahtavi.betterflight.common.BetterFlightCommonConfig;
import com.rejahtavi.betterflight.common.Sounds;
import com.rejahtavi.betterflight.events.ClientEvents;
import com.rejahtavi.betterflight.events.CommonEvents;
import com.rejahtavi.betterflight.network.FlightMessages;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;


@Mod(BetterFlight.MODID)
public class BetterFlight
{

    // Mod identification data
    public static final String MODID = "betterflight";
    public static final String MODNAME = "Better Flight";
    public static final String VERSION = "2.1.3-beta";

    // Optional dependencies state
    public static boolean isCuriousElytraLoaded = false;
    public static boolean isBeanBackpackLoaded = false;

    // Constructor & initialization
    public BetterFlight()
    {
        final IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();

        Sounds.SOUNDS.register(eventBus);

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, BetterFlightCommonConfig.SERVER_SPEC, MODID + "-server.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientConfig.CLIENT_SPEC, MODID + "-client.toml");

        eventBus.addListener(this::onCommonSetupEvent);
        eventBus.addListener(this::onModConfigEvent);
        eventBus.addListener(this::onLoadCompleteEvent);

        if (FMLEnvironment.dist == Dist.CLIENT)
        {
            MinecraftForge.EVENT_BUS.register(ClientEvents.class);
            MinecraftForge.EVENT_BUS.register(ClassicHudOverlay.class);
            MinecraftForge.EVENT_BUS.register(this);
            eventBus.addListener(this::onClientSetupEvent);
        }

        MinecraftForge.EVENT_BUS.register(CommonEvents.class);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onCommonSetupEvent(FMLCommonSetupEvent event)
    {
        FlightMessages.register();
    }

    @SubscribeEvent
    public void onClientSetupEvent(FMLClientSetupEvent event)
    {
        ClientEvents.init();
    }

    @SubscribeEvent
    public void onModConfigEvent(final ModConfigEvent evt)
    {
        if (evt.getConfig().getModId().equals(MODID))
        {
            if (evt.getConfig().getType() == Type.SERVER)
                BetterFlightCommonConfig.bake();
            if (evt.getConfig().getType() == Type.CLIENT)
                ClientConfig.bake();
        }
    }

    @SubscribeEvent
    public void onLoadCompleteEvent(FMLLoadCompleteEvent event)
    {
        if ((ModList.get().isLoaded("elytraslot")
                && ModList.get().isLoaded("curios")))
        {
            isCuriousElytraLoaded = true;
        }
        if (ModList.get().isLoaded("beansbackpacks"))
        {
            isBeanBackpackLoaded = true;
        }
    }
}

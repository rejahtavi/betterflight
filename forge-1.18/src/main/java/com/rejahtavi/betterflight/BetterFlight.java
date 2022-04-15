package com.rejahtavi.betterflight;

import com.rejahtavi.betterflight.client.ClientConfig;
import com.rejahtavi.betterflight.client.ClientLogic;
import com.rejahtavi.betterflight.client.HUDOverlay;
import com.rejahtavi.betterflight.common.ServerConfig;
import com.rejahtavi.betterflight.common.ServerLogic;
import com.rejahtavi.betterflight.network.CFlightActionPacket;
import com.rejahtavi.betterflight.network.SElytraChargePacket;

import net.minecraft.resources.ResourceLocation;
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
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

@Mod(BetterFlight.MODID)
public class BetterFlight {

    // Mod identification data
    public static final String MODID = "betterflight";
    public static final String MODNAME = "Better Flight";
    public static final String VERSION = "0.6.2";

    // Network Channel
    public static final SimpleChannel NETWORK = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(MODID, "networking")).clientAcceptedVersions(s -> true)
            .serverAcceptedVersions(s -> true).networkProtocolVersion(() -> VERSION).simpleChannel();

    // Optional dependencies state
    public static boolean isCuriousElytraLoaded = false;

    // Constructor & initialization
    public BetterFlight() {
        final IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ServerConfig.SERVER_SPEC, MODID + "-server.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientConfig.CLIENT_SPEC, MODID + "-client.toml");

        eventBus.addListener(this::onCommonSetupEvent);
        eventBus.addListener(this::onModConfigEvent);
        eventBus.addListener(this::onLoadCompleteEvent);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            MinecraftForge.EVENT_BUS.register(ClientLogic.class);
            MinecraftForge.EVENT_BUS.register(HUDOverlay.class);
            eventBus.addListener(this::onClientSetupEvent);
        }

        MinecraftForge.EVENT_BUS.register(ServerLogic.class);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onCommonSetupEvent(FMLCommonSetupEvent event) {
        NETWORK.registerMessage(0, CFlightActionPacket.class,
                CFlightActionPacket::encode,
                CFlightActionPacket::decode,
                CFlightActionPacket::onPacketReceived);
        NETWORK.registerMessage(1, SElytraChargePacket.class,
                SElytraChargePacket::encode,
                SElytraChargePacket::decode,
                SElytraChargePacket::onPacketReceived);
    }

    @SubscribeEvent
    public void onClientSetupEvent(FMLClientSetupEvent event) {
        ClientLogic.init();
    }

    @SubscribeEvent
    public void onModConfigEvent(final ModConfigEvent evt) {
        if (evt.getConfig().getModId().equals(MODID)) {
            if (evt.getConfig().getType() == Type.SERVER)
                ServerConfig.bake();
            if (evt.getConfig().getType() == Type.CLIENT)
                ClientConfig.bake();
        }
    }

    @SubscribeEvent
    public void onLoadCompleteEvent(FMLLoadCompleteEvent event) {
        if ((ModList.get().isLoaded("curiouselytra")
                && ModList.get().isLoaded("curios"))) {
            isCuriousElytraLoaded = true;
        }
    }
}

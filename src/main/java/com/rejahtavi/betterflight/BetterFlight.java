package com.rejahtavi.betterflight;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.rejahtavi.betterflight.client.HUDOverlay;
import com.rejahtavi.betterflight.client.InputHandler;
import com.rejahtavi.betterflight.common.FlightActions;
import com.rejahtavi.betterflight.config.Config;
import com.rejahtavi.betterflight.network.CFlightActionPacket;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.config.ModConfig.ModConfigEvent;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import net.minecraftforge.registries.ForgeRegistries;

@Mod(BetterFlight.MODID)
public class BetterFlight {

    // Mod identification data
    public static final String MODID = "betterflight";
    public static final String MODNAME = "Better Flight";
    public static final String VERSION = "0.5";

    // Logging handler
    public static final Logger LOGGER = LogManager.getLogger(MODID);
    
    // Network Channel
    public static final SimpleChannel NETWORK = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(MODID, "networking")).clientAcceptedVersions(s -> true)
            .serverAcceptedVersions(s -> true).networkProtocolVersion(() -> VERSION).simpleChannel();
    
    // Stores handle to the ender dragon flap sound, which we speed up for flapping
    public static SoundEvent FLAP_SOUND;
    
    // Used for making decisions based on presence of curious elytra mod
    // (requires both curious elytra and curios to be loaded)
    public static boolean isCuriousElytraLoaded = false;

    // Mod Constructor / init
    public BetterFlight() {
        final IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();

        eventBus.addListener(this::onCommonSetupEvent);
        eventBus.addListener(this::onModConfigEvent);
        eventBus.addListener(this::onLoadCompleteEvent);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.COMMON_SPEC, MODID + ".toml");

        if (FMLEnvironment.dist == Dist.CLIENT) {
            MinecraftForge.EVENT_BUS.register(InputHandler.class);
            MinecraftForge.EVENT_BUS.register(HUDOverlay.class);
            eventBus.addListener(this::onClientSetupEvent);
            FLAP_SOUND = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("minecraft", "entity.ender_dragon.flap"));
        }

        MinecraftForge.EVENT_BUS.register(FlightActions.class);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onCommonSetupEvent(FMLCommonSetupEvent event) {
        // Register our packet type, used for client->server updates
        NETWORK.registerMessage(0, CFlightActionPacket.class,
                CFlightActionPacket::encode,
                CFlightActionPacket::decode,
                CFlightActionPacket::onPacketReceived);
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public void onClientSetupEvent(FMLClientSetupEvent event) {
        // Registers keybinds and client-side event handlers
        InputHandler.init();
    }

    @SubscribeEvent
    public void onModConfigEvent(final ModConfigEvent evt) {
        if (evt.getConfig().getModId().equals(MODID)) {
            if (evt.getConfig().getType() == Type.COMMON)
                // rebake the configuration any time the mod config is changed
                Config.bake();
        }
    }

    @SubscribeEvent
    public void onLoadCompleteEvent(FMLLoadCompleteEvent event) {
        if ((ModList.get().isLoaded("curiouselytra")
                && ModList.get().isLoaded("curios"))) {
            // if both mods are present, we need to scan curio slots for elytras too
            isCuriousElytraLoaded = true;
        }
    }
    

    public static void DEBUG(String s) {
        LOGGER.log(Level.WARN, s);
    }
}

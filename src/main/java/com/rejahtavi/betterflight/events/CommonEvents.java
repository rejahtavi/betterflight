package com.rejahtavi.betterflight.events;

import com.rejahtavi.betterflight.BetterFlight;
import com.rejahtavi.betterflight.common.BetterFlightCommonConfig;
import com.rejahtavi.betterflight.network.BetterFlightMessages;
import com.rejahtavi.betterflight.network.STCElytraChargePacket;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod.EventBusSubscriber(modid = BetterFlight.MODID, value = Dist.DEDICATED_SERVER)
public class CommonEvents {


    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        STCElytraChargePacket.send(event.getEntity(), BetterFlightCommonConfig.maxCharge);
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        STCElytraChargePacket.send(event.getEntity(), BetterFlightCommonConfig.maxCharge);
    }
    
    @SubscribeEvent
    public static void onPlayerChangeGameMode(PlayerEvent.PlayerChangeGameModeEvent event) {
        STCElytraChargePacket.send(event.getEntity(), BetterFlightCommonConfig.maxCharge);
    }
    @SubscribeEvent
    public void onCommonSetupEvent(FMLCommonSetupEvent event) {
        BetterFlightMessages.register();
    }
}

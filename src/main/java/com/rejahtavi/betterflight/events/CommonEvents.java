package com.rejahtavi.betterflight.events;

import com.rejahtavi.betterflight.BetterFlight;
import com.rejahtavi.betterflight.common.BetterFlightCommonConfig;
import com.rejahtavi.betterflight.network.FlightMessages;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod.EventBusSubscriber(modid = BetterFlight.MODID, value = Dist.DEDICATED_SERVER)
public class CommonEvents
{


    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event)
    {
        FlightMessages.sendToPlayer(BetterFlightCommonConfig.maxCharge, (ServerPlayer) event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event)
    {
        FlightMessages.sendToPlayer(BetterFlightCommonConfig.maxCharge, (ServerPlayer) event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerChangeGameMode(PlayerEvent.PlayerChangeGameModeEvent event)
    {
        FlightMessages.sendToPlayer(BetterFlightCommonConfig.maxCharge, (ServerPlayer) event.getEntity());
    }

    @SubscribeEvent
    public void onCommonSetupEvent(FMLCommonSetupEvent event)
    {
        FlightMessages.register();
    }
}

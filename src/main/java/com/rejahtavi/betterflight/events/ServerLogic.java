package com.rejahtavi.betterflight.events;

import com.rejahtavi.betterflight.BetterFlight;
import com.rejahtavi.betterflight.common.CommonEvents;
import com.rejahtavi.betterflight.network.SElytraChargePacket;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BetterFlight.MODID, value = Dist.DEDICATED_SERVER)
public class ServerLogic {


    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        SElytraChargePacket.send(event.getEntity(), CommonEvents.maxCharge);
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        SElytraChargePacket.send(event.getEntity(), CommonEvents.maxCharge);
    }
    
    @SubscribeEvent
    public static void onPlayerChangeGameMode(PlayerEvent.PlayerChangeGameModeEvent event) {
        SElytraChargePacket.send(event.getEntity(), CommonEvents.maxCharge);
    }

    // TODO: Possibly a 'get altitude' function, and an option to restrict
    // elytra flap effectiveness based on distance above terrain.
    // Perhaps add some sort of bonus for navigating close to the ground
    // or other obstacles? could be fun :)
}

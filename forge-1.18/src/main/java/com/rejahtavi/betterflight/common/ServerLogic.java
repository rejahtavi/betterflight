package com.rejahtavi.betterflight.common;

import java.util.function.Supplier;

import com.rejahtavi.betterflight.BetterFlight;
import com.rejahtavi.betterflight.client.ClientConfig;
import com.rejahtavi.betterflight.network.CFlightActionPacket;
import com.rejahtavi.betterflight.network.SElytraChargePacket;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkEvent;

@Mod.EventBusSubscriber(modid = BetterFlight.MODID, value = Dist.DEDICATED_SERVER)
public class ServerLogic {

    private static final Vec3 NORMAL_UP = new Vec3(0.0D, 1.0D, 0.0D);

    // These static methods implement the actual flight behaviors.
    // They are used by both client and server.
    //
    // The client calls them directly in response to player input,
    // then immediately sends a CFlightUpdatePacket to the server.
    //
    // The server responds by running the exact same method on
    // to keep server state in sync with the client's requests.

    public static void applyTakeOffImpulse(Player player) {
        // take offs need no forward component, due to the player already sprinting.
        // they do need additional vertical thrust to reliably get the player
        // enough time to flap away before hitting the ground again.
        Vec3 verticalThrust = NORMAL_UP.scale(ServerConfig.TAKE_OFF_THRUST);
        player.startFallFlying();
        player.setDeltaMovement(player.getDeltaMovement().add(verticalThrust));

        // this plays the sound to everyone EXCEPT the player it is invoked on.
        // the player's copy of the sound is handled on the client side.
        player.playSound(Sounds.FLAP, (float) ClientConfig.takeOffVolume, ClientConfig.FLAP_SOUND_PITCH);
    }

    public static void applyFlapImpulse(Player player) {
        // grant a small amount of forward thrust along with each vertical boost
        Vec3 verticalThrust = NORMAL_UP.scale(ServerConfig.FLAP_THRUST);
        Vec3 forwardThrust = player.getDeltaMovement().normalize().scale(ServerConfig.FLAP_THRUST * 0.25);
        player.setDeltaMovement(player.getDeltaMovement().add(forwardThrust).add(verticalThrust));

        // this plays the sound to everyone EXCEPT the player it is invoked on.
        // the player's copy of the sound is handled on the client side.
        player.playSound(Sounds.FLAP, (float) ClientConfig.flapVolume, ClientConfig.FLAP_SOUND_PITCH);
    }

    public static void applyFlareImpulse(Player player) {
        // simplified drag equation = (a bunch of constants) * velocity squared
        // ignore all the constants and just use a single coefficient from config
        Vec3 dragDirection = player.getDeltaMovement().normalize().reverse();
        double velocitySquared = player.getDeltaMovement().lengthSqr();
        Vec3 dragThrust = dragDirection.scale(velocitySquared * ServerConfig.FLARE_DRAG);
        player.setDeltaMovement(player.getDeltaMovement().add(dragThrust));
    }

    public static void applyElytraRechargeFoodCost(Player player) {
        // each tick of recharge on the meter costs food
        player.causeFoodExhaustion((float) ServerConfig.exhaustionPerChargePoint);
    }

    public static void handleCFlightActionPacket(CFlightActionPacket message, Supplier<NetworkEvent.Context> context) {
        switch (message.getUpdateType()) {
            case TAKEOFF:
                applyTakeOffImpulse(context.get().getSender());
                break;
            case FLAP:
                applyFlapImpulse(context.get().getSender());
                break;
            case FLARE:
                applyFlareImpulse(context.get().getSender());
                break;
            case RECHARGE:
                applyElytraRechargeFoodCost(context.get().getSender());
                break;
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        SElytraChargePacket.send(event.getPlayer(), ServerConfig.maxCharge);
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        SElytraChargePacket.send(event.getPlayer(), ServerConfig.maxCharge);
    }
    
    @SubscribeEvent
    public static void onPlayerChangeGameMode(PlayerEvent.PlayerChangeGameModeEvent event) {
        SElytraChargePacket.send(event.getPlayer(), ServerConfig.maxCharge);
    }

    // TODO: Possibly a 'get altitude' function, and an option to restrict
    // elytra flap effectiveness based on distance above terrain.
    // Perhaps add some sort of bonus for navigating close to the ground
    // or other obstacles? could be fun :)
}

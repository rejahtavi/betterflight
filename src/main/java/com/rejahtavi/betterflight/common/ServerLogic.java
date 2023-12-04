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
        Vec3 upwards = new Vec3(0.0D,ServerConfig.TAKE_OFF_THRUST,0.0D).scale(getCeilingFactor(player));
        player.startFallFlying();
        player.push(upwards.x,upwards.y,upwards.z);

        // this plays the sound to everyone EXCEPT the player it is invoked on.
        // the player's copy of the sound is handled on the client side.
        player.playSound(Sounds.FLAP.get(), (float) ClientConfig.takeOffVolume, ClientConfig.FLAP_SOUND_PITCH);
    }

    /**
     * grant a small amount of forward thrust along with each vertical boost
     * @param player
     */
    public static void applyFlapImpulse(Player player) {
        double ceilingFactor = getCeilingFactor(player);
        Vec3 upwards = new Vec3(0.0D,ServerConfig.FLAP_THRUST,0.0D).scale(getCeilingFactor(player));
        Vec3 forwards = player.getDeltaMovement().normalize().scale(ServerConfig.FLAP_THRUST * 0.25).scale(ceilingFactor);
        Vec3 impulse = forwards.add(upwards);
        player.push(impulse.x,impulse.y,impulse.z);

        // this plays the sound to everyone EXCEPT the player it is invoked on.
        // the player's copy of the sound is handled on the client side.
        player.playSound(Sounds.FLAP.get(), (float) ClientConfig.flapVolume, ClientConfig.FLAP_SOUND_PITCH);
    }

    /**
     * simplified drag equation = (a bunch of constants) * velocity squared
     * ignore all the constants and just use a single coefficient from config
     * @param player
     */
    public static void applyFlareImpulse(Player player) {

        Vec3 dragDirection = player.getDeltaMovement().normalize().reverse();
        double velocitySquared = player.getDeltaMovement().lengthSqr();
        Vec3 dragThrust = dragDirection.scale(velocitySquared * ServerConfig.FLARE_DRAG);
        player.push(dragThrust.x,dragThrust.y,dragThrust.z);
    }

    /**
     * converts food into flight stamina
     * @param player
     */
    public static void applyElytraRechargeFoodCost(Player player) {
        // each tick of recharge on the meter costs food
        player.causeFoodExhaustion((float) ServerConfig.exhaustionPerChargePoint);
    }

    /**
     * determines flight power when reaching soft and hard altitude limits
     * @param player
     * @return 1.0d-0.0d based on distance between hard limit and player
     */
    public static double getCeilingFactor(Player player) {
        double altitude = player.getY();
        // flying low, full power
        if (altitude < ServerConfig.softCeiling) {
            return 1.0D;
        }

        // flying too high, no power
        if (altitude > ServerConfig.hardCeiling) {
            return 0.0D;
        }

        // flying in between, scale power accordingly
        return (altitude - ServerConfig.softCeiling) / ServerConfig.ceilingRange;
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
        SElytraChargePacket.send(event.getEntity(), ServerConfig.maxCharge);
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        SElytraChargePacket.send(event.getEntity(), ServerConfig.maxCharge);
    }
    
    @SubscribeEvent
    public static void onPlayerChangeGameMode(PlayerEvent.PlayerChangeGameModeEvent event) {
        SElytraChargePacket.send(event.getEntity(), ServerConfig.maxCharge);
    }

    // TODO: Possibly a 'get altitude' function, and an option to restrict
    // elytra flap effectiveness based on distance above terrain.
    // Perhaps add some sort of bonus for navigating close to the ground
    // or other obstacles? could be fun :)
}

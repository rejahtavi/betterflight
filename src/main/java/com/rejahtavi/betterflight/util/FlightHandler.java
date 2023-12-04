package com.rejahtavi.betterflight.util;

import com.rejahtavi.betterflight.client.ClientConfig;
import com.rejahtavi.betterflight.common.CommonEvents;
import com.rejahtavi.betterflight.common.Sounds;
import com.rejahtavi.betterflight.network.CFlightActionPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class FlightHandler {

    // These static methods implement the actual flight behaviors.
    // They are used by both client and server.
    //
    // The client calls them directly in response to player input,
    // then immediately sends a CFlightUpdatePacket to the server.
    //
    // The server responds by running the exact same method on
    // to keep server state in sync with the client's requests.

    public static void handleTakeoff(Player player) {
        // take offs need no forward component, due to the player already sprinting.
        // they do need additional vertical thrust to reliably get the player
        // enough time to flap away before hitting the ground again.
        Vec3 upwards = new Vec3(0.0D, CommonEvents.TAKE_OFF_THRUST,0.0D).scale(getCeilingFactor(player));
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
    public static void handleFlap(Player player) {
        double ceilingFactor = getCeilingFactor(player);
        Vec3 upwards = new Vec3(0.0D, CommonEvents.FLAP_THRUST,0.0D).scale(getCeilingFactor(player));
        Vec3 forwards = player.getDeltaMovement().normalize().scale(CommonEvents.FLAP_THRUST * 0.25).scale(ceilingFactor);
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
    public static void handleFlare(Player player) {

        Vec3 dragDirection = player.getDeltaMovement().normalize().reverse();
        double velocitySquared = player.getDeltaMovement().lengthSqr();
        Vec3 dragThrust = dragDirection.scale(velocitySquared * CommonEvents.FLARE_DRAG);
        player.push(dragThrust.x,dragThrust.y,dragThrust.z);
    }

    /**
     * converts food into flight stamina by adding exhaustion to the player
     * @param player
     */
    public static void handleFlightStaminaExhaustion(Player player) {
        player.getFoodData().addExhaustion((float) CommonEvents.exhaustionPerChargePoint);
    }

    /**
     * determines flight power when reaching soft and hard altitude limits
     * @param player
     * @return 1.0d-0.0d based on distance between hard limit and player
     */
    public static double getCeilingFactor(Player player) {
        double altitude = player.getY();
        // flying low, full power
        if (altitude < CommonEvents.softCeiling) {
            return 1.0D;
        }
        // flying too high, no power
        if (altitude > CommonEvents.hardCeiling) {
            return 0.0D;
        }
        // flying in between, scale power accordingly
        return (altitude - CommonEvents.softCeiling) / CommonEvents.ceilingRange;
    }

    public static void handleCFlightActionPacket(CFlightActionPacket message, Supplier<NetworkEvent.Context> context) {
        switch (message.getUpdateType()) {
            case TAKEOFF:
                handleTakeoff(context.get().getSender());
                break;
            case FLAP:
                handleFlap(context.get().getSender());
                break;
            case FLARE:
                handleFlare(context.get().getSender());
                break;
            case RECHARGE:
                handleFlightStaminaExhaustion(context.get().getSender());
                break;
        }
    }
}

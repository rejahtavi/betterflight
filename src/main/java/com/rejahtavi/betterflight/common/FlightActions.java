package com.rejahtavi.betterflight.common;

import java.util.function.Supplier;

import com.rejahtavi.betterflight.BetterFlight;
import com.rejahtavi.betterflight.config.Config;
import com.rejahtavi.betterflight.network.CFlightActionPacket;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.fml.network.NetworkEvent.Context;

// implements all the actual new flight mechanics
public class FlightActions {

    private static final Vector3d NORMAL_UP = new Vector3d(0.0D, 1.0D, 0.0D);

    // These static methods implement the actual flight behaviors.
    // They are used by both client and server.
    //
    // The client calls them directly in response to player input,
    // then immediately sends a CFlightUpdatePacket to the server.
    //
    // The server responds by running the exact same method on
    // the ServerPlayerEntity to keep it in sync with the client's
    // requests. This should also avoid any "kicked because flying
    // is disabled on this server" issues with the new flight
    // mechanics.

    public static void applyTakeOffImpulse(PlayerEntity player) {
        // take offs need no forward component, due to the player already sprinting.
        // they do need additional vertical thrust to reliably get the player
        // enough clearance to boost away before hitting the ground again.
        Vector3d verticalThrust = NORMAL_UP.scale(Config.TAKE_OFF_THRUST);
        player.startFallFlying();
        player.setDeltaMovement(player.getDeltaMovement().add(verticalThrust));

        // take offs have substantial food cost to get off the ground
        player.causeFoodExhaustion((float) (Config.elytraExhaustionRate * Config.TAKE_OFF_FOOD_MULTIPLIER));

        // take offs are a bit louder than normal flight
        // this plays the sound to everyone EXCEPT the player it is invoked on.
        // the player's copy of the sound is handled on the client side.
        player.playSound(BetterFlight.FLAP_SOUND, (float) Config.TAKE_OFF_VOLUME, 2.0f);
    }

    public static void applyFlapImpulse(PlayerEntity player) {
        // grant a small amount of forward thrust along with each vertical boost
        Vector3d verticalThrust = NORMAL_UP.scale(Config.FLAP_THRUST);
        Vector3d forwardThrust = player.getDeltaMovement().normalize().scale(Config.FLAP_THRUST * 0.25);
        player.setDeltaMovement(player.getDeltaMovement().add(forwardThrust).add(verticalThrust));
        player.causeFoodExhaustion((float) (Config.elytraExhaustionRate));

        // normal flaps are pretty quiet to avoid becoming annoying
        // this plays the sound to everyone EXCEPT the player it is invoked on.
        // the player's copy of the sound is handled on the client side.
        player.playSound(BetterFlight.FLAP_SOUND, (float) Config.FLAP_VOLUME, 2.0f);
    }

    public static void applyFlareImpulse(PlayerEntity player) {
        // simplified drag equation = (a bunch of constants) * velocity squared
        // we can ignore all the constants and just use a single coefficient from config
        Vector3d dragDirection = player.getDeltaMovement().normalize().reverse();
        double velocitySquared = player.getDeltaMovement().lengthSqr();
        Vector3d dragThrust = dragDirection.scale(velocitySquared * Config.FLARE_DRAG);
        player.setDeltaMovement(player.getDeltaMovement().add(dragThrust));

        // tick for tick, flaring costs the same as flapping
        player.causeFoodExhaustion((float) Config.elytraExhaustionRate / (Config.elytraCooldownTicks));
    }

    public static void handleCFlightActionPacket(CFlightActionPacket message, Supplier<Context> context) {
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
        }
    }

    // TODO: Possibly a 'get altitude' function, and an option to restrict
    // elytra flap effectiveness based on distance above terrain.
    // Perhaps add some sort of bonus for navigating close to the ground
    // or other obstacles? could be fun :)
}

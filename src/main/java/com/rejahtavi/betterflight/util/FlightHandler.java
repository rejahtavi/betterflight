package com.rejahtavi.betterflight.util;

import com.rejahtavi.betterflight.common.BetterFlightCommonConfig;
import com.rejahtavi.betterflight.common.FlightActionType;
import com.rejahtavi.betterflight.network.FlightMessages;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class FlightHandler
{

    // These static methods implement the actual flight behaviors.
    // They are used by both client and server.
    //
    // The client calls them directly in response to player input,
    // then immediately sends a CFlightUpdatePacket to the server.
    //
    // The server responds by running the exact same method on
    // to keep server state in sync with the client's requests.

    public static void handleClassicTakeoff(Player player)
    {
        // take offs need no forward component, due to the player already sprinting.
        // they do need additional vertical thrust to reliably get the player
        // enough time to flap away before hitting the ground again.
        Vec3 upwards = new Vec3(0.0D, BetterFlightCommonConfig.TAKE_OFF_THRUST, 0.0D).scale(getCeilingFactor(player));
        player.startFallFlying();
        player.push(upwards.x, upwards.y, upwards.z);

        // this plays the sound to everyone EXCEPT the player it is invoked on.
        // the player's copy of the sound is handled on the client side.
        FlightMessages.sendToServer(FlightActionType.TAKEOFF);
    }

    /**
     * grant a small amount of forward thrust along with each vertical boost
     *
     * @param player
     */
    public static void handleClassicFlap(Player player)
    {
        double ceilingFactor = getCeilingFactor(player);
        Vec3 upwards = new Vec3(0.0D, BetterFlightCommonConfig.CLASSIC_FLAP_THRUST, 0.0D).scale(getCeilingFactor(player));
        Vec3 forwards = player.getDeltaMovement().normalize().scale(BetterFlightCommonConfig.CLASSIC_FLAP_THRUST * 0.25).scale(ceilingFactor);
        Vec3 impulse = forwards.add(upwards);
        player.push(impulse.x, impulse.y, impulse.z);
        FlightMessages.sendToServer(FlightActionType.FLAP);
    }

    /**
     * simplified drag equation = (a bunch of constants) * velocity squared
     * ignore all the constants and just use a single coefficient from config
     *
     * @param player
     * @side client
     */
    public static void handleFlare(Player player)
    {
        Vec3 dragDirection = player.getDeltaMovement().normalize().reverse();
        double velocitySquared = player.getDeltaMovement().lengthSqr();
        Vec3 dragThrust = dragDirection.scale(velocitySquared * BetterFlightCommonConfig.FLARE_DRAG);

        double fallingSpeed = player.getDeltaMovement().y();
        if (fallingSpeed < 0)
        {
            player.push(dragThrust.x, dragThrust.y - (fallingSpeed * .10), dragThrust.z);
        } else
        {
            player.push(dragThrust.x, dragThrust.y, dragThrust.z);
        }
    }

    /**
     * converts food into flight stamina by adding exhaustion to the player
     *
     * @param player
     * @side server
     */
    public static void handleFlightStaminaExhaustion(Player player)
    {
        player.causeFoodExhaustion((float) BetterFlightCommonConfig.exhaustionPerChargePoint);
    }

    /**
     * determines flight power when reaching soft and hard altitude limits
     *
     * @param player
     * @return 1.0d-0.0d based on distance between hard limit and player
     */
    private static double getCeilingFactor(Player player)
    {
        double altitude = player.getY();
        // flying low, full power
        if (altitude < BetterFlightCommonConfig.softCeiling)
        {
            return 1.0D;
        }
        // flying too high, no power
        if (altitude > BetterFlightCommonConfig.hardCeiling)
        {
            return 0.0D;
        }
        // flying in between, scale power accordingly
        return (altitude - BetterFlightCommonConfig.softCeiling) / BetterFlightCommonConfig.ceilingRange;
    }

    /**
     * Pushes player in looking vector weakly, mimicking wings. Tells server to play sound at player position
     *
     * @param player
     * @side client
     */
    public static void handleModernFlap(Player player)
    {
        double d0 = 0.1; //delta coefficient. Influenced by difference between d0 and current delta
        double d1 = 0.55; //boost coefficient
        Vec3 looking = player.getLookAngle();
        Vec3 delta = player.getDeltaMovement();

        Vec3 impulse = (delta.add(
                looking.x * d1 + (looking.x * d0 - delta.x) * 1.5,
                looking.y * d1 + (looking.y * d0 - delta.y) * 1.5,
                looking.z * d1 + (looking.z * d0 - delta.z) * 1.5))
                .scale(getCeilingFactor(player))                //scale to ceiling limit
                .add(getUpVector(player).scale(0.25));  //add slight up vector
        player.push(impulse.x, impulse.y, impulse.z);
        FlightMessages.sendToServer(FlightActionType.FLAP);
    }

    /**
     * Pushes player in looking vector strongly. Tells server to play sound at player position
     *
     * @param player
     * @side client
     */
    public static void handleModernBoost(Player player)
    {
        double d0 = 0.1; //delta coefficient. Influenced by difference between d0 and current delta
        double d1 = 1.0; //boost coefficient
        Vec3 looking = player.getLookAngle();
        Vec3 delta = player.getDeltaMovement();

        Vec3 impulse = (delta.add(
                looking.x * d1 + (looking.x * d0 - delta.x) * 1.5,
                looking.y * d1 + (looking.y * d0 - delta.y) * 1.5,
                looking.z * d1 + (looking.z * d0 - delta.z) * 1.5))
                .scale(getCeilingFactor(player))                //scale to ceiling limit
                .add(getUpVector(player).scale(0.25));  //add slight up vector

        player.push(impulse.x, impulse.y, impulse.z);
        FlightMessages.sendToServer(FlightActionType.BOOST);
    }

    /**
     * Returns a unit vector normal to the player's looking vector.
     *
     * @param player
     * @return Vec3 normal
     */
    private static Vec3 getUpVector(Player player)
    {
        float yaw = player.getYRot() % 360;
        double rads = yaw * (Math.PI / 180);
        Vec3 left = new Vec3(Math.cos(rads), 0, Math.sin(rads));
        Vec3 up = player.getLookAngle().cross(left);
        return up;
    }

    public static void handleFlightStop()
    {
        FlightMessages.sendToServer(FlightActionType.STOP);
    }
}

package com.rejahtavi.betterflight.network;

import com.rejahtavi.betterflight.common.FlightActionType;
import com.rejahtavi.betterflight.common.Sounds;
import com.rejahtavi.betterflight.util.FlightHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client->Server packet, keeps server up to date when a client flaps an elytra
 */
public class CTSFlightEffectsPacket
{

    private final FlightActionType flightUpdate;

    public CTSFlightEffectsPacket(FlightActionType flightUpdate)
    {
        this.flightUpdate = flightUpdate;
    }

    public void encode(FriendlyByteBuf buffer)
    {
        buffer.writeEnum(this.flightUpdate);
    }

    public FlightActionType getUpdateType()
    {
        return this.flightUpdate;
    }

    public static CTSFlightEffectsPacket decode(FriendlyByteBuf buffer)
    {
        return new CTSFlightEffectsPacket(buffer.readEnum(FlightActionType.class));
    }

    public static void handle(CTSFlightEffectsPacket message, Supplier<NetworkEvent.Context> supplier)
    {
        NetworkEvent.Context context = supplier.get();
        Player player = context.getSender();

        context.enqueueWork(() ->
        {
            if (player == null)
                return;
            switch (message.getUpdateType())
            {
                //Movement logic handled on client. This packet tells the server to play the sound.
                case FLAP:
                    playSound(player, Sounds.FLAP.get(), 0.5F, 2F);
                    break;
                case BOOST:
                    playSound(player, Sounds.BOOST.get(), 2F, 1F);
                    break;
                case TAKEOFF:
                    player.startFallFlying();
                    playSound(player, Sounds.FLAP.get(), 1F, 2F);
                    break;
                case STOP:
                    player.stopFallFlying();
                    break;
                case RECHARGE:
                    FlightHandler.handleFlightStaminaExhaustion(player);
                    break;
            }
        });
        context.setPacketHandled(true);
    }

    private static void playSound(Player player, SoundEvent sound, float volume, float pitch)
    {
        try
        {
            player.level().playSound(null, BlockPos.containing(player.position()), sound,
                    SoundSource.PLAYERS, volume, pitch);
        } catch (Exception e)
        {

            throw new RuntimeException("No level found");
        }
    }
}

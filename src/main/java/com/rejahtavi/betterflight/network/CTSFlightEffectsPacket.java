package com.rejahtavi.betterflight.network;

import java.util.function.Supplier;

import com.rejahtavi.betterflight.client.ClientConfig;
import com.rejahtavi.betterflight.common.FlightActionType;

import com.rejahtavi.betterflight.common.Sounds;
import com.rejahtavi.betterflight.util.FlightHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

/**
 * Client->Server packet, keeps server up to date when a client flaps an elytra
  */
public class CTSFlightEffectsPacket
{

    private FlightActionType flightUpdate;

    public CTSFlightEffectsPacket(FlightActionType flightUpdate) {
        this.flightUpdate = flightUpdate;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.flightUpdate);
    }

    public FlightActionType getUpdateType() {
        return this.flightUpdate;
    }

    public static CTSFlightEffectsPacket decode(FriendlyByteBuf buffer) {
        return new CTSFlightEffectsPacket(buffer.readEnum(FlightActionType.class));
    }

    public static void handle(CTSFlightEffectsPacket message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        Player player = context.getSender();
        context.enqueueWork(() -> {
            switch (message.getUpdateType()) {
                //Movement logic handled on client. This packet tells the server to play the sound.
                case MODERN_FLAP, CLASSIC_FLAP:
                    player.level.playSound(null, new BlockPos(player.position()), Sounds.FLAP.get(),
                            SoundSource.PLAYERS, (float) ClientConfig.flapVolume, ClientConfig.FLAP_SOUND_PITCH);
                    break;
                case BOOST:
                    player.level.playSound(null, new BlockPos(player.position()), Sounds.BOOST.get(),
                            SoundSource.PLAYERS,2F, 1F);
                    break;
                case TAKEOFF:
                    player.startFallFlying();
                    player.level.playSound(null, new BlockPos(player.position()), Sounds.FLAP.get(),
                            SoundSource.PLAYERS, (float) ClientConfig.takeOffVolume, ClientConfig.FLAP_SOUND_PITCH);
                    break;
                case RECHARGE: //Action is actually handled by server
                    FlightHandler.handleFlightStaminaExhaustion(context.getSender());
                    break;
            }
        });
        context.setPacketHandled(true);
    }
}

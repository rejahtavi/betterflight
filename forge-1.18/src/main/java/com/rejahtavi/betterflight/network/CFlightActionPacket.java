package com.rejahtavi.betterflight.network;

import java.util.function.Supplier;

import com.rejahtavi.betterflight.BetterFlight;
import com.rejahtavi.betterflight.common.FlightActionType;
import com.rejahtavi.betterflight.common.ServerLogic;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

// Client->Server packet, keeps server up to date when a client flaps an elytra
public class CFlightActionPacket {

    private FlightActionType flightUpdate;

    public CFlightActionPacket(FlightActionType flightUpdate) {
        this.flightUpdate = flightUpdate;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.flightUpdate);
    }

    public FlightActionType getUpdateType() {
        return this.flightUpdate;
    }

    public static CFlightActionPacket decode(FriendlyByteBuf buffer) {
        return new CFlightActionPacket(buffer.readEnum(FlightActionType.class));
    }

    public static void onPacketReceived(CFlightActionPacket message, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerLogic.handleCFlightActionPacket(message, context);
        });
        context.get().setPacketHandled(true);
    }
    
    public static void send(FlightActionType action) {
        BetterFlight.NETWORK.sendToServer(new CFlightActionPacket(action));
    }
}

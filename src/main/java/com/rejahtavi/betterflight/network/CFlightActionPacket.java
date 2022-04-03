package com.rejahtavi.betterflight.network;

import java.util.function.Supplier;

import com.rejahtavi.betterflight.common.FlightActionType;
import com.rejahtavi.betterflight.common.FlightActions;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent.Context;

// implements our Client->Server packet, used for telling the server how each
// client is behaving, and allowing it to keep the server-side simulation in
// sync with the behaviors implemented by the mod.
public class CFlightActionPacket {

    private FlightActionType flightUpdate;

    public CFlightActionPacket(FlightActionType flightUpdate) {
        this.flightUpdate = flightUpdate;
    }

    public void encode(PacketBuffer buffer) {
        buffer.writeEnum(this.flightUpdate);
    }

    public FlightActionType getUpdateType() {
        return this.flightUpdate;
    }

    public static CFlightActionPacket decode(PacketBuffer buffer) {
        return new CFlightActionPacket(buffer.readEnum(FlightActionType.class));
    }

    // This is a SERVER-SIDE packet handler for receiving FROM clients.
    // No client side is necessary, as the server will never *send* a packet.
    public static void onPacketReceived(CFlightActionPacket message, Supplier<Context> context) {
        context.get().enqueueWork(() -> {
            FlightActions.handleCFlightActionPacket(message, context);
        });
        context.get().setPacketHandled(true);
    }
}

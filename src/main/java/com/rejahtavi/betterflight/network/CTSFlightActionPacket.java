package com.rejahtavi.betterflight.network;

import java.util.function.Supplier;

import com.rejahtavi.betterflight.BetterFlight;
import com.rejahtavi.betterflight.common.FlightActionType;

import com.rejahtavi.betterflight.util.FlightHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/**
 * Client->Server packet, keeps server up to date when a client flaps an elytra
  */
public class CTSFlightActionPacket {

    private FlightActionType flightUpdate;

    public CTSFlightActionPacket(FlightActionType flightUpdate) {
        this.flightUpdate = flightUpdate;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.flightUpdate);
    }

    public FlightActionType getUpdateType() {
        return this.flightUpdate;
    }

    public static CTSFlightActionPacket decode(FriendlyByteBuf buffer) {
        return new CTSFlightActionPacket(buffer.readEnum(FlightActionType.class));
    }

    public static void handle(CTSFlightActionPacket message, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            switch (message.getUpdateType()) {
                case MODERN_FLAP:
                    FlightHandler.handleModernFlap(context.get().getSender());
                    break;
                case BOOST:
                    FlightHandler.handleModernBoost(context.get().getSender());
                    break;
                case TAKEOFF:
                    FlightHandler.handleClassicTakeoff(context.get().getSender());
                    break;
                case CLASSIC_FLAP:
                    FlightHandler.handleClassicFlap(context.get().getSender());
                    break;
                case FLARE:
                    FlightHandler.handleFlare(context.get().getSender());
                    break;
                case RECHARGE:
                    FlightHandler.handleFlightStaminaExhaustion(context.get().getSender());
                    break;
            }
        });
        context.get().setPacketHandled(true);
    }
    
    public static void send(FlightActionType action) {
        BetterFlight.NETWORK.sendToServer(new CTSFlightActionPacket(action));
    }
}

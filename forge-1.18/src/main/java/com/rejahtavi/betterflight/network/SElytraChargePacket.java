package com.rejahtavi.betterflight.network;

import java.util.function.Supplier;

import com.rejahtavi.betterflight.BetterFlight;
import com.rejahtavi.betterflight.client.ClientLogic;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

// Server->Client packet, sets player elytra charge upon login / respawn
public class SElytraChargePacket {

    private int charge;

    public SElytraChargePacket(int charge) {
        this.charge = charge;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(this.charge);
    }

    public int getCharge() {
        return this.charge;
    }

    public static SElytraChargePacket decode(FriendlyByteBuf buffer) {
        return new SElytraChargePacket(buffer.readInt());
    }

    public static void onPacketReceived(SElytraChargePacket message, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ClientLogic.handleSElytraChargePacket(message));
        });
        context.get().setPacketHandled(true);
    }
    
    public static void send(Player recipient, int charge) {
        ServerPlayer player = (ServerPlayer) recipient;
        BetterFlight.NETWORK.sendTo(new SElytraChargePacket(charge),
                player.connection.getConnection(),
                NetworkDirection.PLAY_TO_CLIENT);
    }
}

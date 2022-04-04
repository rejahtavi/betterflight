package com.rejahtavi.betterflight.network;

import java.util.function.Supplier;

import com.rejahtavi.betterflight.BetterFlight;
import com.rejahtavi.betterflight.client.ClientLogic;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent.Context;

// Server->Client packet, sets player elytra charge upon login / respawn
public class SElytraChargePacket {

    private int charge;

    public SElytraChargePacket(int charge) {
        this.charge = charge;
    }

    public void encode(PacketBuffer buffer) {
        buffer.writeInt(this.charge);
    }

    public int getCharge() {
        return this.charge;
    }

    public static SElytraChargePacket decode(PacketBuffer buffer) {
        return new SElytraChargePacket(buffer.readInt());
    }

    public static void onPacketReceived(SElytraChargePacket message, Supplier<Context> context) {
        context.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ClientLogic.handleSElytraChargePacket(message, context));
        });
        context.get().setPacketHandled(true);
    }
    
    public static void send(PlayerEntity recipient, int charge) {
        ServerPlayerEntity player = (ServerPlayerEntity) recipient;
        BetterFlight.NETWORK.sendTo(new SElytraChargePacket(charge),
                player.connection.getConnection(),
                NetworkDirection.PLAY_TO_CLIENT);
    }
}

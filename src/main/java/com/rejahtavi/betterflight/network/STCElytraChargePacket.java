package com.rejahtavi.betterflight.network;

import com.rejahtavi.betterflight.util.InputHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// Server->Client packet, sets player elytra charge upon login / respawn
public class STCElytraChargePacket
{

    private final int charge;

    public STCElytraChargePacket(int charge)
    {
        this.charge = charge;
    }

    public void encode(FriendlyByteBuf buffer)
    {
        buffer.writeInt(this.charge);
    }

    public int getCharge()
    {
        return this.charge;
    }

    public static STCElytraChargePacket decode(FriendlyByteBuf buffer)
    {
        return new STCElytraChargePacket(buffer.readInt());
    }

    public static void handle(STCElytraChargePacket message, Supplier<NetworkEvent.Context> context)
    {
        context.get().enqueueWork(() ->
        {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> InputHandler.charge = message.getCharge());
        });
        context.get().setPacketHandled(true);
    }
}

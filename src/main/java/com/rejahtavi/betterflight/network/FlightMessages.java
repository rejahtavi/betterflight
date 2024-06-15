package com.rejahtavi.betterflight.network;

import com.rejahtavi.betterflight.BetterFlight;
import com.rejahtavi.betterflight.common.FlightActionType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class FlightMessages
{
    private static SimpleChannel NETWORK;
    private static int packetID = 0;

    private static int id()
    {
        return packetID++;
    }

    public static void register()
    {
        SimpleChannel channel = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(BetterFlight.MODID, "networking")).clientAcceptedVersions(s -> true)
                .serverAcceptedVersions(s -> true).networkProtocolVersion(() -> BetterFlight.VERSION).simpleChannel();
        NETWORK = channel;

        NETWORK.messageBuilder(CTSFlightEffectsPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(CTSFlightEffectsPacket::decode)
                .encoder(CTSFlightEffectsPacket::encode)
                .consumerMainThread(CTSFlightEffectsPacket::handle)
                .add();

        NETWORK.messageBuilder(STCElytraChargePacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(STCElytraChargePacket::decode)
                .encoder(STCElytraChargePacket::encode)
                .consumerMainThread(STCElytraChargePacket::handle)
                .add();
    }

    public static void sendToServer(FlightActionType action)
    {
        //preparing for separating action to different packet
        if (action.equals(FlightActionType.RECHARGE))
            NETWORK.sendToServer(new CTSFlightEffectsPacket(action));
        else
            NETWORK.sendToServer(new CTSFlightEffectsPacket(action));
    }

    public static void sendToPlayer(int stamina, ServerPlayer player)
    {
        NETWORK.send(PacketDistributor.PLAYER.with(() -> player), new STCElytraChargePacket(stamina));
    }


}

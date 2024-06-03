package com.rejahtavi.betterflight.network;

import com.rejahtavi.betterflight.BetterFlight;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class BetterFlightMessages
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

        NETWORK.messageBuilder(CTSFlightActionPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(CTSFlightActionPacket::decode)
                .encoder(CTSFlightActionPacket::encode)
                .consumerMainThread(CTSFlightActionPacket::handle)
                .add();

        NETWORK.messageBuilder(STCElytraChargePacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(STCElytraChargePacket::decode)
                .encoder(STCElytraChargePacket::encode)
                .consumerMainThread(STCElytraChargePacket::handle)
                .add();
    }

    public static <MSG> void sendToServer(MSG message)
    {
        NETWORK.sendToServer(message);
    }
    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        NETWORK.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
}

package net.kenji.advanced_ai_villagers.network;

import net.kenji.advanced_ai_villagers.AdvancedAiVillagers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModPacketHandler {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(AdvancedAiVillagers.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    private static int id() {
        return packetId++;
    }

    public static void register() {
        INSTANCE.messageBuilder(ClientTagSyncPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(ClientTagSyncPacket::decode)
                .encoder(ClientTagSyncPacket::encode)
                .consumerMainThread(ClientTagSyncPacket::handle)
                .add();
        INSTANCE.messageBuilder(ServerVoiceMessagePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(ServerVoiceMessagePacket::decode)
                .encoder(ServerVoiceMessagePacket::encode)
                .consumerMainThread(ServerVoiceMessagePacket::handle)
                .add();
    }

    // Helper method to send packet to server
    public static void sendToServer(Object packet) {
        INSTANCE.sendToServer(packet);
    }

    // Helper method to send packet to specific player
    public static void sendToPlayer(Object packet, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    // Helper method to send packet to all players
    public static void sendToAll(Object packet) {
        INSTANCE.send(PacketDistributor.ALL.noArg(), packet);
    }
}
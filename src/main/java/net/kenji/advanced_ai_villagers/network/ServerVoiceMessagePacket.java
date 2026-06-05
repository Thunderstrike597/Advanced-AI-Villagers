package net.kenji.advanced_ai_villagers.network;

import net.kenji.advanced_ai_villagers.api.SpeechManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;
import java.util.UUID;
import java.util.function.Supplier;

public class ServerVoiceMessagePacket {

    final String message;
    // Constructor only needs UUID and boolean - playerPatch is looked up on server
    public ServerVoiceMessagePacket(String message) {
        this.message = message;
    }

    public static void encode(ServerVoiceMessagePacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.message);
    }

    public static ServerVoiceMessagePacket decode(FriendlyByteBuf buf) {
        String text = buf.readUtf();
        return new ServerVoiceMessagePacket(text); // Fixed - matches constructor
    }

    public static void handle(ServerVoiceMessagePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();

            if(player == null)return;
            SpeechManager.sendSpeechMessage(player, packet.message, true);

        });
        ctx.get().setPacketHandled(true);
    }
}
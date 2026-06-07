package net.kenji.ai_talking_villagers.network;

import net.kenji.ai_talking_villagers.api.manager.SpeechManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerVoiceMessagePacket {

    final String message;
    final boolean lookAtVillagerToSpeak;
    // Constructor only needs UUID and boolean - playerPatch is looked up on server
    public ServerVoiceMessagePacket(String message, boolean lookAtVillagerToSpeak) {
        this.message = message;
        this.lookAtVillagerToSpeak = lookAtVillagerToSpeak;
    }

    public static void encode(ServerVoiceMessagePacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.message);
        buf.writeBoolean(packet.lookAtVillagerToSpeak);
    }

    public static ServerVoiceMessagePacket decode(FriendlyByteBuf buf) {
        String text = buf.readUtf();
        boolean lookAtVillagerToSpeak = buf.readBoolean();
        return new ServerVoiceMessagePacket(text, lookAtVillagerToSpeak); // Fixed - matches constructor
    }

    public static void handle(ServerVoiceMessagePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();

            if(player == null)return;
            SpeechManager.sendSpeechMessage(player, packet.message, packet.lookAtVillagerToSpeak);

        });
        ctx.get().setPacketHandled(true);
    }
}
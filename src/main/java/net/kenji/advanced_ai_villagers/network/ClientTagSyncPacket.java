package net.kenji.advanced_ai_villagers.network;

import net.kenji.advanced_ai_villagers.api.SpeechManager;
import net.kenji.advanced_ai_villagers.client.render.TextBubbleRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientTagSyncPacket {
    private final int entityId;
    private final String tagText;


    public ClientTagSyncPacket(int entityId, String tagText) {
        this.entityId = entityId;
        this.tagText = tagText;
    }

    // Encode: Write data to buffer
    public static void encode(ClientTagSyncPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.entityId);
        buf.writeUtf(packet.tagText);
    }

    public static ClientTagSyncPacket decode(FriendlyByteBuf buf) {
        int entityId = buf.readInt();
        String  motionId = buf.readUtf();
        return new ClientTagSyncPacket(entityId, motionId);
    }

    // Handle: Process the packet on the receiving side
    public static void handle(ClientTagSyncPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if(ctx.get().getDirection().getReceptionSide().isClient()) {
                executeOnClient(packet);
            }
        });
        ctx.get().setPacketHandled(true);
    }
    @OnlyIn(Dist.CLIENT)
    private static void executeOnClient(ClientTagSyncPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        Entity entity = player.level().getEntity(packet.entityId);
        if (entity == null) return;

        entity.getPersistentData().putString(SpeechManager.SPEECH_BUBBLE_TAG, packet.tagText);
    }
}
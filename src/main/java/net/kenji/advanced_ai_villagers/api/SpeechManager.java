package net.kenji.advanced_ai_villagers.api;

import net.kenji.advanced_ai_villagers.client.render.TextBubbleRenderer;
import net.kenji.advanced_ai_villagers.network.ClientTagSyncPacket;
import net.kenji.advanced_ai_villagers.network.ModPacketHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SpeechManager {
    public static final Map<UUID, Integer> speechCountMap = new HashMap<>();
    public static final int SPEECH_DECAY_TIME = 160 * 2;

    public static final String SPEECH_BUBBLE_TAG = "villager_speech_bubble";


    public static void addVillagerSpeech(Villager villager, ServerPlayer serverPlayer, String text){
        ModPacketHandler.sendToPlayer(new ClientTagSyncPacket(villager.getId(), text), serverPlayer);
        villager.getPersistentData().putString(SpeechManager.SPEECH_BUBBLE_TAG, text);
    }
}

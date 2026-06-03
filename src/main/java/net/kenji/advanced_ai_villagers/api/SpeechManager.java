package net.kenji.advanced_ai_villagers.api;

import net.kenji.advanced_ai_villagers.client.render.TextBubbleRenderer;
import net.kenji.advanced_ai_villagers.network.ClientTagSyncPacket;
import net.kenji.advanced_ai_villagers.network.ModPacketHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SpeechManager {
    public static final Map<UUID, Thread> threadTrackMap = new HashMap<>();


    public static final Map<UUID, String> speechTrackMap = new HashMap<>();
    public static final Map<UUID, Integer> speechCounterMap = new HashMap<>();

    public static final Map<UUID, Integer> speechCountMap = new HashMap<>();
    public static final int SPEECH_DECAY_TIME = (int)(260 * 1.35);

    public static final String SPEECH_BUBBLE_TAG = "villager_speech_bubble";

    public static final float VILLAGER_TALK_CHANCE = 0.115F;
    public static final int VILLAGER_TALK_COUNTER_MAX = 20;
    public static final int THREAD_MAX_COUNT = 5;


    public static void addVillagerSpeech(Villager villager, Player player, String text){
        if (player instanceof ServerPlayer serverPlayer)
            ModPacketHandler.sendToPlayer(new ClientTagSyncPacket(villager.getId(), text), serverPlayer);
        villager.getPersistentData().putString(SpeechManager.SPEECH_BUBBLE_TAG, text);
    }
}

package net.kenji.ai_talking_villagers.api.manager;

import com.mojang.datafixers.util.Pair;
import net.kenji.ai_talking_villagers.api.context.ContextManager;
import net.kenji.ai_talking_villagers.api.model.VillagerAiModel;
import net.kenji.ai_talking_villagers.network.ClientTagSyncPacket;
import net.kenji.ai_talking_villagers.network.ModPacketHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jline.utils.Log;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SpeechManager {

    public static final ExecutorService aiThreadPool = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });


    public static final Map<UUID, String> speechTrackMap = new HashMap<>();
    public static final Map<UUID, Integer> speechCounterMap = new HashMap<>();
    public static final Map<UUID, Integer> speechCountMap = new HashMap<>();
    public static final Map<UUID, Integer> captionCounterMap = new HashMap<>();

    public static final int SPEECH_DECAY_TIME = (int)(260 * 1.35);

    public static final String PLAYER_SPEECH_TAG = "player_speech";


    public static final String SPEECH_BUBBLE_TAG = "villager_speech_bubble";
    public static final String PERSONALITY_TAG = "villager_personality";
    public static final String RECENT_EVENT_TAG = "villager_recent_event";


    public static final float VILLAGER_TALK_CHANCE = 0.115F;
    public static final int VILLAGER_TALK_COUNTER_MAX = 20;
    public static final int CAPTION_COUNTER_MAX = 120;
    public static final int THREAD_MAX_COUNT = 5;
    public static final double VILLAGER_TALK_DIST_MAX = 4.2F;

    public static final Map<Pair<UUID, UUID>, VillagerConversation> activeConversations = new HashMap<>();

    public static class VillagerConversation {
        public final UUID villager1;
        public final UUID villager2;
        public volatile String lastMessage = null;   // what was just said
        public volatile UUID nextSpeaker;            // whose turn it is
        public volatile boolean active = true;
        public volatile int turnsRemaining;
        public Thread thread;

        public VillagerConversation(UUID v1, UUID v2, int turns) {
            this.villager1 = v1;
            this.villager2 = v2;
            this.nextSpeaker = v1;  // v1 always opens
            this.turnsRemaining = turns;
        }
    }

    public static void addVillagerSpeech(Villager villager, Player player, String text){
        if (player instanceof ServerPlayer serverPlayer)
            ModPacketHandler.sendToPlayer(new ClientTagSyncPacket(villager.getId(), text), serverPlayer);
        villager.getPersistentData().putString(SpeechManager.SPEECH_BUBBLE_TAG, text);
    }

    public static void sendSpeechMessage(ServerPlayer player, String message, boolean useLookAngle) {
        Villager nearest = useLookAngle
                ? findLookAtVillager(player, 10)
                : findNearestVillager(player, 10);

        if (nearest == null) return; // no villager targeted, ignore

        List<Villager> villagerGroup = findVillagerGroup(player, 10, 2.5F, nearest);

        SpeechManager.aiThreadPool.submit(() -> {
            villagerGroup.forEach((villager) -> {
                String context = ContextManager.getPlayerChatContext(villager, player);

                String response = VillagerAiModel.generateResponse(message, context, VillagerAiModel.TEMPERATURE_CHAT, 25, villager.getUUID());
                Log.info("Response: " + response);
                // Replace name placeholder
                response = response.replace("VILLAGER_NAME", villager.getName().getString());
                response = response.replace("VILLAGERNAME", villager.getName().getString());
                response = response.replace("VILLAGER NAME", villager.getName().getString());

                if (!response.isEmpty()) {
                    String finalResponse = response;
                    villager.getServer().execute(() -> {
                        SpeechManager.addVillagerSpeech(villager, player, finalResponse);
                    });
                }
            });
        });
    }

    private static List<Villager> findVillagerGroup(ServerPlayer player, double range,double nearbyRange, Villager villagerToIgnore) {
        List<Villager> villagers = new ArrayList<>(player.level().getEntitiesOfClass(
                Villager.class,
                player.getBoundingBox().inflate(range)
        ).stream().toList());

        villagers.add(villagerToIgnore);

        List<Villager> finalVillagers = new ArrayList<>();
        List<Villager> nearbyVillagers = villagerToIgnore.level().getEntitiesOfClass(
                Villager.class,
                villagerToIgnore.getBoundingBox().inflate(nearbyRange)
        );
        nearbyVillagers.forEach(villager ->{
            if(player.position().distanceTo(villager.position()) <= range)
                finalVillagers.add(villager);
        });
        if(!finalVillagers.contains(villagerToIgnore))
            finalVillagers.add(villagerToIgnore);

        return finalVillagers;
    }
    private static Villager findNearestVillager(ServerPlayer player, double range) {
        return player.level().getEntitiesOfClass(
                        Villager.class,
                        player.getBoundingBox().inflate(range)
                ).stream()
                .min(Comparator.comparingDouble(v -> v.distanceTo(player)))
                .orElse(null);
    }
    private static Villager findLookAtVillager(ServerPlayer player, double range) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();

        return player.level().getEntitiesOfClass(
                        Villager.class,
                        player.getBoundingBox().inflate(range)
                ).stream().filter(v -> {
                    Vec3 toVillager = v.getEyePosition().subtract(eyePos).normalize();
                    double dot = lookVec.dot(toVillager);
                    return dot > 0.866F;
                })
                .min(Comparator.comparingDouble(v -> v.distanceTo(player)))
                .orElse(null);
    }
}

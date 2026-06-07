package net.kenji.ai_talking_villagers.api.handler;

import com.mojang.datafixers.util.Pair;
import net.kenji.ai_talking_villagers.api.manager.SpeechManager;
import net.kenji.ai_talking_villagers.api.context.ContextManager;
import net.kenji.ai_talking_villagers.api.model.VillagerAiModel;
import net.minecraft.world.entity.npc.Villager;

import java.util.UUID;

public class VillagerConversationHandler {
    public static void startVillagerConversation(Villager speaker, Villager listener) {
        Pair<UUID, UUID> pairKey = normalisedPair(speaker.getUUID(), listener.getUUID());
        if (SpeechManager.activeConversations.containsKey(pairKey)) return;
        if (SpeechManager.activeConversations.size() >= SpeechManager.THREAD_MAX_COUNT) return; // <-- add this

        SpeechManager.VillagerConversation conv = new SpeechManager.VillagerConversation(
                speaker.getUUID(), listener.getUUID(), 6
        );

        // Capture both references on the server thread NOW, before the thread starts
        // This is safe because we're still on the server tick thread here
        Villager v1 = speaker;
        Villager v2 = listener;

        Thread thread = new Thread(() -> {
            try {
                String context = ContextManager.getVillagerChatContext(v1);
                String opening = VillagerAiModel.generateVillagerGreeting(
                        context, VillagerAiModel.TEMPERATURE_CHAT, 25, v1.getUUID()
                );

                if (opening.isEmpty() || Thread.currentThread().isInterrupted()) return;

                conv.lastMessage = opening;
                conv.nextSpeaker = v2.getUUID(); // v2 replies next
                postSpeech(v1, opening);
                sleepOrAbort(conv, SpeechManager.SPEECH_DECAY_TIME * 50L);

                while (conv.active && conv.turnsRemaining > 0 && !Thread.currentThread().isInterrupted()) {

                    // Pick who speaks this turn based on nextSpeaker
                    boolean isV1Turn = conv.nextSpeaker.equals(v1.getUUID());
                    Villager responder = isV1Turn ? v1 : v2;
                    Villager other    = isV1Turn ? v2 : v1;

                    // Safety: bail if either villager has been removed from the world
                    if (!responder.isAlive() || !other.isAlive()) break;
                    if (responder.position().distanceTo(other.position()) > SpeechManager.VILLAGER_TALK_DIST_MAX) break;

                    String context2 = ContextManager.getVillagerChatContext(responder);
                    String reply = VillagerAiModel.generateVillagerReply(
                            conv.lastMessage,
                            context2,
                            VillagerAiModel.TEMPERATURE_CHAT,
                            25,
                            responder.getUUID(),
                            other.getUUID()
                    );

                    if (reply.isEmpty() || Thread.currentThread().isInterrupted()) break;

                    conv.lastMessage = reply;
                    conv.nextSpeaker = other.getUUID(); // swap turn
                    conv.turnsRemaining--;

                    postSpeech(responder, reply);
                    sleepOrAbort(conv, SpeechManager.SPEECH_DECAY_TIME * 50L);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                SpeechManager.activeConversations.remove(pairKey);
                VillagerAiModel.clearVillagerConversation(v1.getUUID(), v2.getUUID());
            }
        });

        thread.setDaemon(true);
        conv.thread = thread;
        SpeechManager.activeConversations.put(pairKey, conv);
        SpeechManager.aiThreadPool.execute(thread);
    }

    // Post speech back onto the server thread safely
    private static void postSpeech(Villager villager, String message) {
        villager.getServer().execute(() ->
                villager.level().players().forEach(player ->
                        SpeechManager.addVillagerSpeech(villager, player, message)
                )
        );
    }

    // Sleep for the bubble display duration, but bail early if conversation cancelled
    private static void sleepOrAbort(SpeechManager.VillagerConversation conv, long ms) throws InterruptedException {
        long end = System.currentTimeMillis() + ms;
        while (System.currentTimeMillis() < end) {
            if (!conv.active || Thread.currentThread().isInterrupted()) throw new InterruptedException();
            Thread.sleep(100);
        }
    }

    // Utility: find a loaded villager entity by UUID near another entity
    private static Villager findVillager(Villager near, UUID targetId) {
        return near.level().getEntitiesOfClass(
                Villager.class,
                near.getBoundingBox().inflate(16),
                v -> v.getUUID().equals(targetId)
        ).stream().findFirst().orElse(null);
    }

    // Normalised pair so (A,B) and (B,A) map to the same key
    public static Pair<UUID, UUID> normalisedPair(UUID a, UUID b) {
        return a.compareTo(b) < 0 ? new Pair<>(a, b) : new Pair<>(b, a);
    }

}

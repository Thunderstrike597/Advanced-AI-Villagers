package net.kenji.advanced_ai_villagers.events;

import com.mojang.datafixers.util.Pair;
import net.kenji.advanced_ai_villagers.AdvancedAiVillagers;
import net.kenji.advanced_ai_villagers.api.SpeechManager;
import net.kenji.advanced_ai_villagers.api.context.ContextManager;
import net.kenji.advanced_ai_villagers.api.model.VillagerAiModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = AdvancedAiVillagers.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TickEvents {


    @SubscribeEvent
    public static void onVillagerTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Villager villager)) return;

        manageVillagerSpeechBubble(villager);
        manageVillagerToVillagerSpeaking(villager);
    }

    public static void manageVillagerToVillagerSpeaking(Villager villager){
        Optional<LivingEntity> target = villager.getBrain().getMemory(MemoryModuleType.INTERACTION_TARGET);
        int counter = SpeechManager.speechCounterMap.getOrDefault(villager.getUUID(), 0);
        if(counter <= 0) {
            SpeechManager.speechCounterMap.put(villager.getUUID(), SpeechManager.VILLAGER_TALK_COUNTER_MAX);
            return;
        }
        else SpeechManager.speechCounterMap.put(villager.getUUID(), counter - 1);


        boolean isTalkingToVillager =
                target.isPresent() && target.get() instanceof Villager villagerTarget;


        if (isTalkingToVillager) {
            float randomChance = villager.getRandom().nextFloat();

            Villager villagerTarget = (Villager) target.get(); // safe after instanceof check above
            Pair<UUID, UUID> pairKey = normalisedPair(villager.getUUID(), villagerTarget.getUUID());
            double dist = villager.position().distanceTo(villagerTarget.position());

            if(dist > SpeechManager.VILLAGER_TALK_DIST_MAX) return;


            if (!villager.getPersistentData().getString(SpeechManager.SPEECH_BUBBLE_TAG).isEmpty()) return;


            if (SpeechManager.activeConversations.containsKey(pairKey))return;
            if (randomChance > SpeechManager.VILLAGER_TALK_CHANCE) return;

            startVillagerConversation(villager, villagerTarget);
        }
    }
    private static void startVillagerConversation(Villager speaker, Villager listener) {
        Pair<UUID, UUID> pairKey = normalisedPair(speaker.getUUID(), listener.getUUID());
        if (SpeechManager.activeConversations.containsKey(pairKey)) return;

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
                sleepOrAbort(conv, SpeechManager.SPEECH_DECAY_TIME / 50L);

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
        thread.start();
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
    private static Pair<UUID, UUID> normalisedPair(UUID a, UUID b) {
        return a.compareTo(b) < 0 ? new Pair<>(a, b) : new Pair<>(b, a);
    }


    public static boolean isVillagerConversationValid(UUID uuid1, UUID uuid2){
        return SpeechManager.threadTrackMap.get(new Pair<>(uuid1, uuid2)) != null;
    }

    public static void manageVillagerSpeechBubble(Villager villager){
        String tagText = villager.getPersistentData().getString(SpeechManager.SPEECH_BUBBLE_TAG);
        if (tagText.isEmpty()) return;

        if(!tagText.equals(SpeechManager.speechTrackMap.getOrDefault(villager.getUUID(), ""))){
            SpeechManager.speechTrackMap.put(villager.getUUID(), tagText);
            SpeechManager.speechCountMap.put(villager.getUUID(), SpeechManager.SPEECH_DECAY_TIME);
        }


        int villagerDecayCounter = SpeechManager.speechCountMap.getOrDefault(villager.getUUID(), -1);
        if (villagerDecayCounter == -1) {
            SpeechManager.speechCountMap.put(villager.getUUID(), SpeechManager.SPEECH_DECAY_TIME);
        } else if (villagerDecayCounter > 0) {
            SpeechManager.speechCountMap.put(villager.getUUID(), villagerDecayCounter - 1);
        } else {
            villager.level().players().forEach(player -> {
                SpeechManager.addVillagerSpeech(villager, player, "");
                Thread villagerAiThreadSpeechManager = SpeechManager.threadTrackMap.get(villager.getUUID());
                if(villagerAiThreadSpeechManager != null) {
                    villagerAiThreadSpeechManager.interrupt();
                }
            });
            SpeechManager.speechCountMap.remove(villager.getUUID());
        }

    }
}
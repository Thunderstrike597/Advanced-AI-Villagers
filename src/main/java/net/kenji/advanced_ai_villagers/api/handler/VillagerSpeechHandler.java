package net.kenji.advanced_ai_villagers.api.handler;

import com.mojang.datafixers.util.Pair;
import net.kenji.advanced_ai_villagers.api.manager.SpeechManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;

import java.util.Optional;
import java.util.UUID;

public class VillagerSpeechHandler {

    public static void tick(Villager villager){
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
            Pair<UUID, UUID> pairKey = VillagerConversationHandler.normalisedPair(villager.getUUID(), villagerTarget.getUUID());
            double dist = villager.position().distanceTo(villagerTarget.position());

            if(dist > SpeechManager.VILLAGER_TALK_DIST_MAX) return;
            if (!villager.getPersistentData().getString(SpeechManager.SPEECH_BUBBLE_TAG).isEmpty()) return;


            if (SpeechManager.activeConversations.containsKey(pairKey))return;
            if (randomChance > SpeechManager.VILLAGER_TALK_CHANCE) return;

            VillagerConversationHandler.startVillagerConversation(villager, villagerTarget);
        }
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

            });
            SpeechManager.speechCountMap.remove(villager.getUUID());
            SpeechManager.speechTrackMap.remove(villager.getUUID()); // <-- add this
        }
    }
}
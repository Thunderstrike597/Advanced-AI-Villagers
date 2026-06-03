package net.kenji.advanced_ai_villagers.events;

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

@Mod.EventBusSubscriber(modid = AdvancedAiVillagers.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TickEvents {


    @SubscribeEvent
    public static void onVillagerTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Villager villager)) return;

        manageVillagerSpeechBubble(villager);
        manageVillagerToVillagerTrading(villager);
    }

    public static void manageVillagerToVillagerTrading(Villager villager){
        Optional<LivingEntity> target = villager.getBrain().getMemory(MemoryModuleType.INTERACTION_TARGET);
        int counter = SpeechManager.speechCounterMap.getOrDefault(villager.getUUID(), 0);
        if(counter <= 0) {
            SpeechManager.speechCounterMap.put(villager.getUUID(), SpeechManager.VILLAGER_TALK_COUNTER_MAX);
            return;
        }
        else SpeechManager.speechCounterMap.put(villager.getUUID(), counter - 1);


        boolean isTalkingToVillager =
                target.isPresent() && target.get() instanceof Villager villagerTarget;

        if(isTalkingToVillager){
            float randomChance = villager.getRandom().nextFloat();

            if(!villager.getPersistentData().getString(SpeechManager.SPEECH_BUBBLE_TAG).isEmpty())return;

            if(randomChance > SpeechManager.VILLAGER_TALK_CHANCE) return;
            if(SpeechManager.threadTrackMap.get(villager.getUUID()) != null) return;
            if(SpeechManager.threadTrackMap.size() >= SpeechManager.THREAD_MAX_COUNT)return;

            Thread aiThread = new Thread(() -> {
                try {
                    if (Thread.currentThread().isInterrupted()) {
                        return;
                    }
                    String context = ContextManager.getVillagerChatContext(villager);

                    if (Thread.currentThread().isInterrupted()) {
                        return;
                    }
                    String response = VillagerAiModel.generateGreeting(context, VillagerAiModel.TEMPERATURE_CHAT, 25, villager.getUUID());

                    if (Thread.currentThread().isInterrupted()) {
                        return;
                    }
                    if (!response.isEmpty()) {
                        String finalResponse = response;
                        villager.getServer().execute(() -> {
                            villager.level().players().forEach(player -> {

                                SpeechManager.addVillagerSpeech(villager, player, finalResponse);
                            });
                        });
                    }
                }
                finally {
                    SpeechManager.threadTrackMap.remove(villager.getUUID());
                }
            });
            aiThread.setDaemon(true);
            aiThread.start();
            SpeechManager.threadTrackMap.put(villager.getUUID(), aiThread);
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
                Thread villagerAiThreadSpeechManager = SpeechManager.threadTrackMap.get(villager.getUUID());
                if(villagerAiThreadSpeechManager != null) {
                    villagerAiThreadSpeechManager.interrupt();
                }
            });
            SpeechManager.speechCountMap.remove(villager.getUUID());
        }

    }
}
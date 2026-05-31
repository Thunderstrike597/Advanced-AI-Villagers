package net.kenji.advanced_ai_villagers.events;

import net.kenji.advanced_ai_villagers.AdvancedAiVillagers;
import net.kenji.advanced_ai_villagers.api.SpeechManager;
import net.kenji.advanced_ai_villagers.client.render.TextBubbleRenderer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jline.utils.Log;

import java.util.Comparator;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = AdvancedAiVillagers.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TickEvents {


    @SubscribeEvent
    public static void onVillagerTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Villager villager)) return;

        String tagText = villager.getPersistentData().getString(SpeechManager.SPEECH_BUBBLE_TAG);
        if (tagText.isEmpty()) return;

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
        }
    }
}
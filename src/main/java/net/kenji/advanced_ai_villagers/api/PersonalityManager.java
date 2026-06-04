package net.kenji.advanced_ai_villagers.api;

import net.kenji.advanced_ai_villagers.AdvancedAiVillagers;
import net.kenji.advanced_ai_villagers.api.context.villager_info.PersonalityContext;
import net.minecraft.world.entity.npc.Villager;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AdvancedAiVillagers.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PersonalityManager {


    @SubscribeEvent
    public static void onLevelJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof Villager villager))
            return;

        if (!villager.getPersistentData()
                .contains(SpeechManager.PERSONALITY_TAG)) {

            PersonalityContext context =
                    PersonalityContext.getRandomPersonality(villager);

            villager.getPersistentData().putString(
                    SpeechManager.PERSONALITY_TAG,
                    context.getContextName());
        }
    }
}

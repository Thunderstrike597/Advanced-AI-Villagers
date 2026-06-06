package net.kenji.advanced_ai_villagers.api.handler;

import net.kenji.advanced_ai_villagers.AiTalkingVillagers;
import net.kenji.advanced_ai_villagers.api.context.villager_info.PersonalityContext;
import net.kenji.advanced_ai_villagers.api.manager.SpeechManager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AiTalkingVillagers.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PersonalityHandler {


    public static void maybeAssignPersonality(Villager villager){

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

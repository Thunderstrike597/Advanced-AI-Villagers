package net.kenji.advanced_ai_villagers.api.context.villager_info;

import net.kenji.advanced_ai_villagers.api.SpeechManager;
import net.kenji.advanced_ai_villagers.api.interfaces.IContext;
import net.minecraft.world.entity.npc.Villager;
import org.jline.utils.Log;

public enum PersonalityContext implements IContext {
    REGULAR(1),
    JOLLY(0.5),
    RUDE(0.25);

    final double personalityChance;

    PersonalityContext(double personalityChance){
        this.personalityChance = personalityChance;
    }
    public static PersonalityContext getContext(Villager villager){
        try{
            String tagString = villager.getPersistentData().getString(SpeechManager.PERSONALITY_TAG);
            return PersonalityContext.valueOf(tagString.toUpperCase());
        }catch (Exception e){
            Log.info("Personality Exception Caught: " + e);
            return REGULAR;
        }
    }
    public static PersonalityContext getRandomPersonality(Villager villager){
        double chance = villager.getRandom().nextDouble();

        for(int i = PersonalityContext.values().length - 1; i >= 0; i--){
            PersonalityContext context = PersonalityContext.values()[i];
            if(chance <= context.personalityChance)
                return context;
        }
        return REGULAR;
    }

    @Override
    public String getContextType() {
        return "Personality";
    }

    @Override
    public String getContextName() {
        String text = this.toString().toLowerCase().replace("_", " ");

        return Character.toUpperCase(text.charAt(0))
                + text.substring(1);
    }
}

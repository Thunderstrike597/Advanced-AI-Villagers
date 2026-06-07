package net.kenji.ai_talking_villagers.api.context.environment;

import net.kenji.ai_talking_villagers.api.interfaces.IContext;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.schedule.Activity;

public enum ActivityContext implements IContext {
    WANDERING,
    WORKING,
    SLEEPING,
    SOCIALIZING,
    TRADING;


    ActivityContext(){ }

    public static ActivityContext getContext(Villager villager){
        ActivityContext activity;

        Brain<?> brain = villager.getBrain();

        if(villager.isTrading()) {
            activity = TRADING;
        }
        else if(villager.isSleeping()) {
            activity = SLEEPING;
        }
        else if(brain.isActive(Activity.WORK)) {
            activity = WORKING;
        }
        else if(brain.isActive(Activity.MEET)) {
            activity = SOCIALIZING;
        }
        else {
            activity = WANDERING;
        }
        return activity;
    }

    @Override
    public String getContextType() {
        return "Activity";
    }

    @Override
    public String getContextName() {
        String text = this.toString().toLowerCase().replace("_", " ");

        return Character.toUpperCase(text.charAt(0))
                + text.substring(1);
    }
}

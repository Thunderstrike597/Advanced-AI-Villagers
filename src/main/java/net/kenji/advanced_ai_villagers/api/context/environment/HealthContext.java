package net.kenji.advanced_ai_villagers.api.context.environment;

import net.kenji.advanced_ai_villagers.api.interfaces.IContext;
import net.minecraft.world.entity.npc.Villager;

public enum HealthContext implements IContext {
    FULL(82),
    HURT(50),
    CRITICAL(1);


    float healthPercent;

    HealthContext(float healthPercent){
        this.healthPercent = healthPercent;
    }
    public static HealthContext getHealthContext(Villager villager){
        float percent = (villager.getHealth() / villager.getMaxHealth()) * 100f;

        for(HealthContext context : HealthContext.values()){
            if(percent >= context.healthPercent){
                return context;
            }
        }
        return CRITICAL;
    }

    @Override
    public String getContextType() {
        return "Health";
    }

    @Override
    public String getContextName() {
        String text = this.toString().toLowerCase().replace("_", " ");

        return Character.toUpperCase(text.charAt(0))
                + text.substring(1);
    }
}

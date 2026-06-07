package net.kenji.ai_talking_villagers.api.context.villager_info;

import net.kenji.ai_talking_villagers.api.interfaces.IContext;
import net.minecraft.world.entity.npc.Villager;

public enum AgeContext implements IContext {
    CHILD,
    ADULT;

    AgeContext(){ }
    public static AgeContext getContext(Villager villager){
        if(villager.isBaby())return CHILD;
        return ADULT;
    }

    @Override
    public String getContextType() {
        return "Age";
    }

    @Override
    public String getContextName() {
        String text = this.toString().toLowerCase().replace("_", " ");

        return Character.toUpperCase(text.charAt(0))
                + text.substring(1);
    }
}

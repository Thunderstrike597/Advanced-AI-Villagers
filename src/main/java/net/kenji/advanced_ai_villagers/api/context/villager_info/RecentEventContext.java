package net.kenji.advanced_ai_villagers.api.context.villager_info;

import net.kenji.advanced_ai_villagers.api.interfaces.IContext;
import net.minecraft.world.entity.npc.Villager;

public enum RecentEventContext implements IContext {
    NONE(""),
    RAID_SURVIVED("raid_survived"),
    VILLAGER_DEATH("villager_death");


    final String tagName;

    RecentEventContext(String tagName){
        this.tagName = tagName;
    }

    public String getTagName(){
        return tagName;
    }

    public static RecentEventContext getContext(Villager villager){
        return NONE;
    }

    @Override
    public String getContextType() {
        return "RecentEvent";
    }

    @Override
    public String getContextName() {
        String text = this.toString().toLowerCase().replace("_", " ");

        return Character.toUpperCase(text.charAt(0))
                + text.substring(1);
    }
}

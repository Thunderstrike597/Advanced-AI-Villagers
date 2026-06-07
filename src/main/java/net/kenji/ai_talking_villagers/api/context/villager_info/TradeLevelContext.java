package net.kenji.ai_talking_villagers.api.context.villager_info;

import net.kenji.ai_talking_villagers.api.interfaces.IContext;
import net.minecraft.world.entity.npc.Villager;

public enum TradeLevelContext implements IContext {
    NOVICE(0),
    APPRENTICE(1),
    JOURNEYMAN(2),
    EXPERT(3),
    MASTER(4);

    final int level;
    TradeLevelContext(int level){
        this.level = level;
    }

    public static TradeLevelContext getContext(Villager villager){
        for (TradeLevelContext level : TradeLevelContext.values()) {
            if (level.level == villager.getVillagerData().getLevel()) return level;
        }
        return NOVICE;
    }


    @Override
    public String getContextType() {
        return "TradeLevel";
    }

    @Override
    public String getContextName() {
        String text = this.toString().toLowerCase().replace("_", " ");

        return Character.toUpperCase(text.charAt(0))
                + text.substring(1);
    }
}

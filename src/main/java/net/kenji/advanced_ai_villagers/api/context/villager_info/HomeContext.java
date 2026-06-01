package net.kenji.advanced_ai_villagers.api.context.villager_info;

import net.kenji.advanced_ai_villagers.api.interfaces.IContext;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;

public enum HomeContext implements IContext {
    HOUSED,
    HOMELESS;



    HomeContext(){ }

    public static HomeContext getContext(Villager villager){
       boolean hasBed = villager.getBrain()
               .getMemory(MemoryModuleType.HOME)
               .isPresent();

        return hasBed ? HOUSED : HOMELESS;
    }


    @Override
    public String getContextType() {
        return "Home";
    }

    @Override
    public String getContextName() {
        String text = this.toString().toLowerCase().replace("_", " ");

        return Character.toUpperCase(text.charAt(0))
                + text.substring(1);
    }
}

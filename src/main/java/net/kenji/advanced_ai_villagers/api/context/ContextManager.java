package net.kenji.advanced_ai_villagers.api.context;

import net.kenji.advanced_ai_villagers.api.context.environment.*;
import net.kenji.advanced_ai_villagers.api.context.villager_info.AgeContext;
import net.kenji.advanced_ai_villagers.api.context.villager_info.HomeContext;
import net.kenji.advanced_ai_villagers.api.context.villager_info.ProfessionContext;
import net.kenji.advanced_ai_villagers.api.context.villager_info.TradeLevelContext;
import net.kenji.advanced_ai_villagers.api.interfaces.IContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;

import java.util.ArrayList;
import java.util.List;

public class ContextManager {
    public static final float VILLAGE_SEARCH_RADIUS = 50;
    public static final int VILLAGER_SEARCH_COUNT = 5;

    public static final double SHELTER_SEARCH_HEIGHT = 12;


    public static String getVillagerChatContext(Villager villager) {
        List<IContext> contexts = new ArrayList<>();
        contexts.add(LocationContext.getLocationContext(villager, VILLAGE_SEARCH_RADIUS, VILLAGER_SEARCH_COUNT));
        contexts.add(TimeContext.getTimeContext(villager));
        contexts.add(ShelterContext.getShelterContext(villager, SHELTER_SEARCH_HEIGHT));
        contexts.add(ThreatContext.getThreatContext(villager));
        contexts.add(HealthContext.getHealthContext(villager));
        contexts.add(ProfessionContext.getProfessionContext(villager));
        contexts.add(AgeContext.getAgeContext(villager));
        contexts.add(TradeLevelContext.getTradeLevelContext(villager));
        contexts.add(AgeContext.getAgeContext(villager));
        contexts.add(HomeContext.getHomeContext(villager));


        List<String> contextNames = new ArrayList<>();
        contexts.forEach(iContext ->{
            if(iContext != null)
                contextNames.add(iContext.getContextType() + "=" + iContext.getContextName());
        });
        contextNames.add("Rep=Neutral");
        return "Context: " + String.join(", ", contextNames);
    }
    public static String getVillagerCombatContext(Villager villager) {
        List<IContext> contexts = new ArrayList<>();
        contexts.add(ThreatContext.getThreatContext(villager));
        contexts.add(HealthContext.getHealthContext(villager));

        List<String> contextNames = new ArrayList<>();
        contexts.forEach(iContext ->{
            if(iContext != null)
                contextNames.add(iContext.getContextType() + "=" + iContext.getContextName());
        });
        contextNames.add("Rep=Neutral");
        return String.join(", ", contextNames);
    }

    private static boolean hasBed(Villager villager) {
        if (!(villager.level() instanceof ServerLevel serverLevel)) return false;

        // Villagers register their bed as a memory
        return villager.getBrain()
                .getMemory(MemoryModuleType.HOME)
                .isPresent();
    }
}

package net.kenji.advanced_ai_villagers.api.context;

import net.kenji.advanced_ai_villagers.api.context.environment.*;
import net.kenji.advanced_ai_villagers.api.context.villager_info.*;
import net.kenji.advanced_ai_villagers.api.interfaces.IContext;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public class ContextManager {
    public static final float VILLAGE_SEARCH_RADIUS = 50;
    public static final int VILLAGER_SEARCH_COUNT = 5;

    public static final double SHELTER_SEARCH_HEIGHT = 12;


    public static String getPlayerChatContext(Villager villager, Player player) {
        List<IContext> contexts = new ArrayList<>();
        contexts.add(PersonalityContext.getContext(villager));
        contexts.add(LocationContext.getContext(villager, VILLAGE_SEARCH_RADIUS, VILLAGER_SEARCH_COUNT));
        contexts.add(TimeContext.getContext(villager));
        contexts.add(ShelterContext.getContext(villager, SHELTER_SEARCH_HEIGHT));
        contexts.add(ThreatContext.getContext(villager));
        contexts.add(HealthContext.getContext(villager));
        contexts.add(ProfessionContext.getContext(villager));
        contexts.add(TradeLevelContext.getContext(villager));
        contexts.add(HomeContext.getContext(villager));
        contexts.add(AgeContext.getContext(villager));
        contexts.add(ReputationContext.getContext(villager, player));
        contexts.add(WeatherContext.getContext(villager));
        contexts.add(ActivityContext.getContext(villager));
        contexts.add(RecentEventContext.getContext(villager));

        List<String> contextNames = new ArrayList<>();
        contexts.forEach(iContext ->{
            if(iContext != null)
                contextNames.add(iContext.getContextType() + "=" + iContext.getContextName());
        });
        return String.join(", ", contextNames);
    }
    public static String getVillagerChatContext(Villager villager) {
        List<IContext> contexts = new ArrayList<>();
        contexts.add(LocationContext.getContext(villager, VILLAGE_SEARCH_RADIUS, VILLAGER_SEARCH_COUNT));
        contexts.add(TimeContext.getContext(villager));
        contexts.add(HealthContext.getContext(villager));
        contexts.add(ProfessionContext.getContext(villager));

        List<String> contextNames = new ArrayList<>();
        contexts.forEach(iContext ->{
            if(iContext != null)
                contextNames.add(iContext.getContextType() + "=" + iContext.getContextName());
        });
        return String.join(", ", contextNames);
    }
    public static String getVillagerCombatContext(Villager villager) {
        List<IContext> contexts = new ArrayList<>();
        contexts.add(ThreatContext.getContext(villager));
        contexts.add(HealthContext.getContext(villager));

        List<String> contextNames = new ArrayList<>();
        contexts.forEach(iContext ->{
            if(iContext != null)
                contextNames.add(iContext.getContextType() + "=" + iContext.getContextName());
        });
        contextNames.add("Rep=Neutral");
        return String.join(", ", contextNames);
    }
}

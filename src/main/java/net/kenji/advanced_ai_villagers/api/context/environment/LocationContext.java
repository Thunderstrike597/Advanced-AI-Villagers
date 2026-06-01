package net.kenji.advanced_ai_villagers.api.context.environment;

import net.kenji.advanced_ai_villagers.api.interfaces.IContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public enum LocationContext implements IContext {
    VILLAGE,
    WILD;

    LocationContext(){ }

    public static LocationContext getLocationContext(Villager villager, double villageSearchRadius, double villagerSearchCount){
        Level level = villager.level();
        if(!(level instanceof ServerLevel serverLevel)) return null;

        List<Villager> villagersInRange = new ArrayList<>();
        villagersInRange.addAll(level.getEntitiesOfClass(
                Villager.class,
                villager.getBoundingBox().inflate(villageSearchRadius)
        ));
        BlockPos nearestVillage = serverLevel.findNearestMapStructure(StructureTags.VILLAGE, villager.getOnPos(), (int)villageSearchRadius, false);

        if(villagersInRange.size() < villagerSearchCount && nearestVillage == null) return LocationContext.WILD;

        return LocationContext.VILLAGE;
    }

    @Override
    public String getContextType() {
        return "Loc";
    }

    @Override
    public String getContextName() {
        String text = this.toString().toLowerCase().replace("_", " ");

        return Character.toUpperCase(text.charAt(0))
                + text.substring(1);
    }
}

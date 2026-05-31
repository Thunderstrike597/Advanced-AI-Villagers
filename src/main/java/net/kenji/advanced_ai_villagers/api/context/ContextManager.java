package net.kenji.advanced_ai_villagers.api.context;

import net.kenji.advanced_ai_villagers.api.interfaces.IContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class ContextManager {
    public static final float VILLAGE_SEARCH_RADIUS = 50;
    public static final int VILLAGE_SEARCH_COUNT = 5;

    public static final double SHELTER_SEARCH_HEIGHT = 12;


    public static String getVillagerContext(Villager villager) {
        List<IContext> contexts = new ArrayList<>();
        contexts.add(getLocationContext(villager));
        contexts.add(getShelterContext(villager));
        contexts.add(getThreatContext(villager));
        if(getLocationContext(villager) != null)
            contexts.add(getTimeContext(villager));

        List<String> contextNames = new ArrayList<>();
        contexts.forEach(iContext ->{
            if(iContext != null)
                contextNames.add(iContext.getContextType() + "=" + iContext.getContextName());
        });
        return "Context: " + String.join(", ", contextNames);
    }

    private static ThreatContext getThreatContext(Villager villager) {
        Brain<Villager> brain = villager.getBrain();

        Optional<LivingEntity> hostile =
                brain.getMemory(MemoryModuleType.NEAREST_HOSTILE);
        ThreatContext context = null;
        if (hostile.isPresent()) {
            LivingEntity threat = hostile.get();

            context = ThreatContext.getThreatContextFormEntity(threat);
        }
        else context = ThreatContext.getThreatContextFormEntity(null);


        return context;
    }

    private static LocationContext getLocationContext(Villager villager){
        Level level = villager.level();
        if(!(level instanceof ServerLevel serverLevel)) return null;

        List<Villager> villagersInRange = new ArrayList<>();
        villagersInRange.addAll(level.getEntitiesOfClass(
                Villager.class,
                villager.getBoundingBox().inflate(VILLAGE_SEARCH_RADIUS)
        ));
        BlockPos nearestVillage = serverLevel.findNearestMapStructure(StructureTags.VILLAGE, villager.getOnPos(), (int)VILLAGE_SEARCH_RADIUS, false);

        if(villagersInRange.size() < VILLAGE_SEARCH_COUNT && nearestVillage == null) return LocationContext.WILD;

        if(!hasBed(villager) && nearestVillage == null) return LocationContext.WILD;

        return LocationContext.VILLAGE;
    }
    private static TimeContext getTimeContext(Villager villager){
        Level level = villager.level();
        return TimeContext.getContextFromTime(level.getDayTime());
    }
    private static ShelterContext getShelterContext(Villager villager) {
        Level level = villager.level();
        BlockPos pos = villager.blockPosition();

        // Check upward for any solid block within 8 blocks above
        for (int i = 1; i <= SHELTER_SEARCH_HEIGHT; i++) {
            BlockPos checkPos = pos.above(i);
            BlockState state = level.getBlockState(checkPos);
            if (state.isSolid()) {
                return ShelterContext.INSIDE; // Something solid is above, they are sheltered
            }
        }
        return ShelterContext.OUTSIDE; // Nothing above, they are outside
    }

    private static boolean hasBed(Villager villager) {
        if (!(villager.level() instanceof ServerLevel serverLevel)) return false;

        // Villagers register their bed as a memory
        return villager.getBrain()
                .getMemory(MemoryModuleType.HOME)
                .isPresent();
    }
}

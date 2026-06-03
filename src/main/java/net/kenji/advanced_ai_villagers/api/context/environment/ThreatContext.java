package net.kenji.advanced_ai_villagers.api.context.environment;

import net.kenji.advanced_ai_villagers.api.interfaces.IContext;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;

import java.util.Optional;

public enum ThreatContext implements IContext {
    NONE(null),
    ZOMBIE(EntityType.ZOMBIE),
    ACTIVE_RAID(null),
    ZOMBIE_VILLAGER(EntityType.ZOMBIE_VILLAGER),
    PILLAGER(EntityType.PILLAGER),
    VINDICATOR(EntityType.VINDICATOR),
    UNKNOWN(null);

    final EntityType<?> entityType;

    ThreatContext(EntityType<?> entityType){
        this.entityType = entityType;
    }

    public static ThreatContext getContext(Villager villager){
        Brain<Villager> brain = villager.getBrain();

        Optional<LivingEntity> hostile =
                brain.getMemory(MemoryModuleType.NEAREST_HOSTILE);
        if (hostile.isPresent()) {
            LivingEntity threat = hostile.get();

            for (ThreatContext context : ThreatContext.values()) {
                if (threat.getType() == context.getEntityType()) {
                    return context;
                }
            }
            return UNKNOWN;
        }

        return NONE;
    }

    public EntityType<?> getEntityType(){
        return this.entityType;
    }
    @Override
    public String getContextType() {
        return "Threat";
    }

    @Override
    public String getContextName() {
        String text = this.toString().toLowerCase().replace("_", " ");

        return Character.toUpperCase(text.charAt(0))
                + text.substring(1);
    }
}

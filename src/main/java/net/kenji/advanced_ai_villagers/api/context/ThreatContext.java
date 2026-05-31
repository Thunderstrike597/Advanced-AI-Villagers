package net.kenji.advanced_ai_villagers.api.context;

import net.kenji.advanced_ai_villagers.api.interfaces.IContext;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public enum ThreatContext implements IContext {
    NONE("None",null),
    ZOMBIE("Zombie", EntityType.ZOMBIE),
    ZOMBIE_VILLAGER("Zombie Villager", EntityType.ZOMBIE_VILLAGER),
    PILLAGER("Pillager", EntityType.PILLAGER),
    VINDICATOR("Vindicator", EntityType.VINDICATOR);

    final EntityType<?> entityType;
    final String contextName;

    ThreatContext(String contextName, EntityType<?> entityType){
        this.contextName = contextName;
        this.entityType = entityType;
    }

    public EntityType<?> getEntityType(){
        return this.entityType;
    }

    public static ThreatContext getThreatContextFormEntity(Entity entity){
        if(entity != null) {
            if (entity.getType() == ZOMBIE.getEntityType()) {
                return ZOMBIE;
            }
            if (entity.getType() == ZOMBIE_VILLAGER.getEntityType()) {
                return ZOMBIE_VILLAGER;
            }
            if (entity.getType() == PILLAGER.getEntityType()) {
                return PILLAGER;
            }
            if (entity.getType() == VINDICATOR.getEntityType()) {
                return VINDICATOR;
            }
        }
        return NONE;
    }
    @Override
    public String getContextType() {
        return "Threat";
    }

    @Override
    public String getContextName(){
        return contextName;
    }
}

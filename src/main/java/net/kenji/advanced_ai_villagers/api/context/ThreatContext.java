package net.kenji.advanced_ai_villagers.api.context;

import net.kenji.advanced_ai_villagers.api.interfaces.IContext;
import net.minecraft.world.entity.EntityType;

public enum ThreatContext implements IContext {
    NONE("",null),
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
    @Override
    public String getContextType() {
        return "Threat";
    }

    @Override
    public String getContextName(){
        return contextName;
    }
}

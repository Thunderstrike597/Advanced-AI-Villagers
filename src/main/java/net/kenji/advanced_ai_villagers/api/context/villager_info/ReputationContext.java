package net.kenji.advanced_ai_villagers.api.context.villager_info;

import net.kenji.advanced_ai_villagers.api.interfaces.IContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public enum ReputationContext implements IContext {
    NEUTRAL,
    POSITIVE,
    NEGATIVE;

    ReputationContext(){ }

    public static ReputationContext getContext(Villager villager, Player player){
        Level level = villager.level();
        if(!(level instanceof ServerLevel serverLevel)) return null;

        int rep = villager.getPlayerReputation(player);

        if(rep > 10){
            return POSITIVE;
        }
        if(rep < -10){
            return NEGATIVE;
        }
        return NEUTRAL;
    }

    @Override
    public String getContextType() {
        return "Rep";
    }

    @Override
    public String getContextName() {
        String text = this.toString().toLowerCase().replace("_", " ");

        return Character.toUpperCase(text.charAt(0))
                + text.substring(1);
    }
}

package net.kenji.advanced_ai_villagers.api.context.environment;

import net.kenji.advanced_ai_villagers.api.interfaces.IContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public enum ShelterContext implements IContext {
    OUTSIDE,
    INSIDE;


    ShelterContext(){ }

    public static ShelterContext getContext(Villager villager, double shelterSearchHeight) {
        Level level = villager.level();
        BlockPos pos = villager.blockPosition();

        // Check upward for any solid block within 8 blocks above
        for (int i = 1; i <= shelterSearchHeight; i++) {
            BlockPos checkPos = pos.above(i);
            BlockState state = level.getBlockState(checkPos);
            if (state.isSolid()) {
                return ShelterContext.INSIDE; // Something solid is above, they are sheltered
            }
        }
        return ShelterContext.OUTSIDE; // Nothing above, they are outside
    }

    @Override
    public String getContextType() {
        return "Shelter";
    }

    @Override
    public String getContextName() {
        String text = this.toString().toLowerCase().replace("_", " ");

        return Character.toUpperCase(text.charAt(0))
                + text.substring(1);
    }
}

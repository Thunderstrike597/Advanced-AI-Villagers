package net.kenji.advanced_ai_villagers.api.context.environment;

import net.kenji.advanced_ai_villagers.api.interfaces.IContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.commands.WeatherCommand;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

public enum WeatherContext implements IContext {
    CLEAR(),
    RAIN(),
    SNOW(),
    THUNDER();


    WeatherContext(){

    }

    public static WeatherContext getContext(Villager villager){
        BlockPos pos = villager.blockPosition();
        Holder<Biome> biome = villager.level().getBiome(pos);

        boolean isSnow = biome.get().getPrecipitationAt(pos) == Biome.Precipitation.SNOW;

        boolean isRainState = villager.level().isRainingAt(pos);

        boolean isRaining = isRainState && !isSnow;
        boolean isSnowing = isRainState && isSnow;

        boolean isThundering = villager.level().isThundering();

        if(isThundering)
            return WeatherContext.THUNDER;

        if(isRaining)
            return WeatherContext.RAIN;
        if(isSnowing)
            return WeatherContext.SNOW;

        return CLEAR;
    }

    @Override
    public String getContextType() {
        return "Weather";
    }

    @Override
    public String getContextName() {
        String text = this.toString().toLowerCase().replace("_", " ");

        return Character.toUpperCase(text.charAt(0))
                + text.substring(1);
    }
}

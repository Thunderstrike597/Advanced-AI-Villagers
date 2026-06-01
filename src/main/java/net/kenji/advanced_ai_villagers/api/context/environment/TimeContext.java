package net.kenji.advanced_ai_villagers.api.context.environment;

import net.kenji.advanced_ai_villagers.api.interfaces.IContext;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;

public enum TimeContext implements IContext {
    MORNING(1000),
    DAY(6000),
    DUSK(12000),
    NIGHT(13000);

    final int time;

    TimeContext(int timeRange){
        this.time = timeRange;
    }
    public static TimeContext getContext(Villager villager) {
        Level level = villager.level();

        long t = level.getDayTime() % 24000;

        TimeContext[] values = TimeContext.values();
        for (int i = values.length - 1; i >= 0; i--) {
            if (t >= values[i].time) return values[i];
        }
        return MORNING;
    }


    @Override
    public String getContextType() {
        return "Time";
    }

    @Override
    public String getContextName() {
        String text = this.toString().toLowerCase().replace("_", " ");

        return Character.toUpperCase(text.charAt(0))
                + text.substring(1);
    }
}

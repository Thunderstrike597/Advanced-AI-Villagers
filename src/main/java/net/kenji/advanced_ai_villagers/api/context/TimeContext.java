package net.kenji.advanced_ai_villagers.api.context;

import com.mojang.datafixers.util.Pair;
import net.kenji.advanced_ai_villagers.api.interfaces.IContext;

public enum TimeContext implements IContext {
    MORNING("Morning", new Pair(0, 6000)),
    DAY("Day", new Pair(6000, 12000)),
    DUSK("Dusk", new Pair(12000, 13000)),
    NIGHT("Night", new Pair(13000, 18000)),
    MIDNIGHT("Midnight", new Pair(18000, 23000)),
    DAWN("Dawn", new Pair(23000, 24000));

    final Pair<Integer, Integer> timeRange;
    final String contextName;
    TimeContext(String contextName, Pair<Integer, Integer> timeRange){
        this.contextName = contextName;
        this.timeRange = timeRange;
    }
    public static TimeContext getContextFromTime(long time) {
        long t = time % 24000; // normalise in case it exceeds 24000
        if (t >= 23000) return DAWN;
        if (t >= 18000) return MIDNIGHT;
        if (t >= 13000) return NIGHT;
        if (t >= 12000) return DUSK;
        if (t >= 6000) return DAY;
        if (t >= 1000) return MORNING;
        return DAWN;
    }

    public Pair<Integer, Integer> getTimeRange(){
        return timeRange;
    }
    @Override
    public String getContextType() {
        return "Time";
    }

    @Override
    public String getContextName(){
        return contextName;
    }
}

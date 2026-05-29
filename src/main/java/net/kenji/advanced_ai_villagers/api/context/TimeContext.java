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
    public static TimeContext getContextFromTime(long time){
        if(time > DAWN.timeRange.getFirst() && time <= DAWN.timeRange.getSecond()){
            return null;
        }
        if(time > MORNING.timeRange.getFirst() && time <= MORNING.timeRange.getSecond()){
            return null;
        }
        if(time > DUSK.timeRange.getFirst() && time <= DUSK.timeRange.getSecond()){
            return DUSK;
        }
        if(time > NIGHT.timeRange.getFirst() && time <= NIGHT.timeRange.getSecond()){
            return NIGHT;
        }
        if(time > MIDNIGHT.timeRange.getFirst() && time <= MIDNIGHT.timeRange.getSecond()){
            return NIGHT;
        }
        return null;
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

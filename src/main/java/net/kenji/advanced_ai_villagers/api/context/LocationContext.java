package net.kenji.advanced_ai_villagers.api.context;

import net.kenji.advanced_ai_villagers.api.interfaces.IContext;

public enum LocationContext implements IContext {
    VILLAGE("Village"),
    WILD("Wild");


    final String contextName;
    LocationContext(String contextName){
        this.contextName = contextName;
    }

    @Override
    public String getContextType() {
        return "Loc";
    }

    @Override
    public String getContextName(){
        return contextName;
    }
}

package net.kenji.advanced_ai_villagers.api.context;

import net.kenji.advanced_ai_villagers.api.interfaces.IContext;

public enum ShelterContext implements IContext {
    OUTSIDE("Outside"),
    INSIDE("Inside");

    final String contextName;
    ShelterContext(String contextName){
        this.contextName = contextName;
    }

    @Override
    public String getContextType() {
        return "Shelter";
    }

    @Override
    public String getContextName(){
        return contextName;
    }
}

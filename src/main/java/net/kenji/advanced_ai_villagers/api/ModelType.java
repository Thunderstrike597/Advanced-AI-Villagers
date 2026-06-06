package net.kenji.advanced_ai_villagers.api;

public enum ModelType {
    GPT_MEDIUM("gpt-medium"),
    GPT_NEO_125M("gpt-neo-125m");

   private final String modelName;

   ModelType(String modelName){
       this.modelName = modelName;
   }

   public String getModelName(){
      return this.modelName;
   }
}

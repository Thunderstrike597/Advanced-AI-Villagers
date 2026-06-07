package net.kenji.ai_talking_villagers;

import net.kenji.ai_talking_villagers.api.ModelType;
import net.minecraftforge.common.ForgeConfigSpec;

public class ConfigCommon {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static ForgeConfigSpec.ConfigValue<ModelType> MODEL_TYPE;

    static {
        BUILDER.push("Models");

        MODEL_TYPE = BUILDER
                .comment("Model Type")
                .defineEnum("The ONNX Model Type Used By Villagers", ModelType.GPT_NEO_125M);

        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}

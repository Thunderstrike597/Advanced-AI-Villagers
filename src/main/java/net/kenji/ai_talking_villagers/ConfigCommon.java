package net.kenji.ai_talking_villagers;

import net.kenji.ai_talking_villagers.api.ModelType;
import net.minecraftforge.common.ForgeConfigSpec;

public class ConfigCommon {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static ForgeConfigSpec.ConfigValue<ModelType> MODEL_TYPE;

    public static ForgeConfigSpec.ConfigValue<Double> MODEL_TEMPERATURE_CHAT;
    public static ForgeConfigSpec.ConfigValue<Double> MODEL_TEMPERATURE_OTHER;
    public static ForgeConfigSpec.ConfigValue<Double> MODEL_REPETITION_PENALTY;

    static {
        BUILDER.push("Models");

        MODEL_TYPE = BUILDER
                .comment("The ONNX model type used by villagers")
                .defineEnum("Model Types", ModelType.GPT_NEO_125M);

        MODEL_TEMPERATURE_CHAT = BUILDER
                .comment("This defines the model temperature for chat situations. In Short, model temperature defines how 'creative' the model is in it's responses.. If too low, the response will be extremely boring and similar, to other responses. If too high it will start rambling nonsense and not make sense. For chatting I decided '0.575' is a good value. Max is 1000 (ONLY if you want to have fun with it lol, I'd suggest between 0.0 - 1.0)")
                .defineInRange("Model Temperature Chat", 0.575D, 0.05D, 1000D);
        MODEL_TEMPERATURE_OTHER = BUILDER
                .comment("This defines the model temperature for non-chat situations. In Short, model temperature defines how 'creative' the model is in it's responses.. If too low, the response will be extremely boring and similar, to other responses. If too high it will start rambling nonsense and not make sense. For chatting I decided '0.575' is a good value. Max is 1000 (ONLY if you want to have fun with it lol, I'd suggest between 0.0 - 1.0)")
                .defineInRange("Model Temperature Other", 0.42D, 0.05D, 1000D);
        MODEL_REPETITION_PENALTY = BUILDER
                .comment("This defines the model repetition penalty. This controls how strongly the model avoids reusing words it has already said. At 1.0 there is no penalty.. Higher values push the model toward more varied vocabulary, but too high can make responses feel unnatural or incoherent. For this I decided 1.12 was a good choice. Max is 100 (Again. ONLY if you want to have fun with it lol, I'd suggest between 1.05 - 1.32)")
                .defineInRange("Model Repetition Penalty", 1.12D, 0.05D, 100D);

        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}

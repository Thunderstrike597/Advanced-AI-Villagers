package net.kenji.ai_talking_villagers;

import net.kenji.ai_talking_villagers.api.ModelType;
import net.minecraftforge.common.ForgeConfigSpec;

public class ConfigClient {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static ForgeConfigSpec.ConfigValue<Boolean> USE_VOICE_RECOGNITON;

    public static ForgeConfigSpec.ConfigValue<Integer> SILENCE_TIMEOUT_MS;
    public static ForgeConfigSpec.ConfigValue<Boolean> SHOW_SPOKEN_CAPTIONS;
    public static ForgeConfigSpec.ConfigValue<Boolean> LOOK_AT_VILLAGER_TO_SPEAK;

    static {
        BUILDER.push("Voice Related");

        USE_VOICE_RECOGNITON = BUILDER
                .comment("Whether or not the mod uses voice recognition to speak to villagers when 'Simple Voice Chat' is present")
                .define("Use Voice Recognition", true);

        LOOK_AT_VILLAGER_TO_SPEAK = BUILDER
                .comment("Whether villagers only reply when being looked at in the general direction")
                .define("Look At Villager To Speak", true);

        SILENCE_TIMEOUT_MS = BUILDER
                .comment("The Time The Microphone must have no input, for voice recognition to pass. (When 'Simple Voice Chat' is present)")
                .defineInRange("Silence Timeout Milliseconds", 300, 10, 2000);

        SHOW_SPOKEN_CAPTIONS = BUILDER
                .comment("Whether or not words spoken appear on the lower left of the screen. (Only used 'Simple Voice Chat' is present)")
                .define("Show Spoken Captions", true);

        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}

package net.kenji.advanced_ai_villagers.api.model;

import net.kenji.advanced_ai_villagers.AdvancedAiVillagers;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;

import static com.mojang.text2speech.Narrator.LOGGER;

public class VillagerTokenizer {

    private HuggingFaceTokenizer tokenizer;
    private static final int EOS_TOKEN = 50256;
    public boolean load() {
        try {
            InputStream stream = VillagerTokenizer.class.getResourceAsStream(
                    "/assets/" + AdvancedAiVillagers.MODID + "/model/tokenizer.json"
            );

            if (stream == null) {
                LOGGER.error("tokenizer.json not found!");
                return false;
            }

            // Copy tokenizer.json to temp file
            Path temp = Files.createTempFile("tokenizer", ".json");
            Files.copy(stream, temp, StandardCopyOption.REPLACE_EXISTING);

            tokenizer = HuggingFaceTokenizer.newInstance(temp);

            LOGGER.info("Tokenizer loaded successfully!");
            return true;

        } catch (Exception e) {
            LOGGER.error("Failed to load tokenizer", e);
            return false;
        }
    }

    public long[] encode(String text) {
        return tokenizer.encode(text).getIds();
    }

    public String decode(long[] tokens) {
        // Decode but remove special tokens
        String text = tokenizer.decode(tokens, true); // true = skip special tokens
        return text;
    }

    public int getEosToken() {
        return EOS_TOKEN;
    }
}
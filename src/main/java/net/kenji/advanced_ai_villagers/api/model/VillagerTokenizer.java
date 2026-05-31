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
            String[] tokenizerFiles = {
                    "tokenizer.json",
                    "tokenizer_config.json",
                    "vocab.json",
                    "merges.txt",
                    "special_tokens_map.json"
            };

            Path tempDir = Files.createTempDirectory("villagerai_tokenizer");

            for (String fileName : tokenizerFiles) {
                InputStream stream = VillagerTokenizer.class.getResourceAsStream(
                        "/assets/" + AdvancedAiVillagers.MODID + "/model/" + fileName
                );
                if (stream == null) {
                    LOGGER.warn("Tokenizer file not found, skipping: " + fileName);
                    continue;
                }
                Files.copy(stream, tempDir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            }

            // Point at the directory, not a single file
            tokenizer = HuggingFaceTokenizer.newInstance(tempDir);

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
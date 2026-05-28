package net.kenji.advanced_ai_villagers.model;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.kenji.advanced_ai_villagers.AdvancedAiVillagers;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class VillagerTokenizer {

    private static final Logger LOGGER = LogManager.getLogger("VillagerTokenizer");

    private Map<String, Integer> encoder;
    private Map<Integer, String> decoder;
    private Map<String, Integer> bpeRanks;
    private Map<Integer, Character> byteDecoder;
    private static final int EOS_TOKEN = 50256;
    private static final int NEWLINE_TOKEN = 198;

    public boolean load() {
        try {
            // Load vocab.json
            InputStream vocabStream = VillagerTokenizer.class
                .getResourceAsStream("/assets/" + AdvancedAiVillagers.MODID + "/model/vocab.json");
            if (vocabStream == null) {
                LOGGER.error("vocab.json not found!");
                return false;
            }

            Gson gson = new Gson();
            Type mapType = new TypeToken<Map<String, Integer>>(){}.getType();
            encoder = gson.fromJson(new InputStreamReader(vocabStream, StandardCharsets.UTF_8), mapType);

            // Build reverse decoder
            decoder = new HashMap<>();
            for (Map.Entry<String, Integer> e : encoder.entrySet()) {
                decoder.put(e.getValue(), e.getKey());
            }

            // Load merges.txt
            InputStream mergesStream = VillagerTokenizer.class
                .getResourceAsStream("/assets/" + AdvancedAiVillagers.MODID + "/model/merges.txt");
            if (mergesStream == null) {
                LOGGER.error("merges.txt not found!");
                return false;
            }

            bpeRanks = new HashMap<>();
            BufferedReader reader = new BufferedReader(new InputStreamReader(mergesStream, StandardCharsets.UTF_8));
            String line;
            int rank = 0;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                if (firstLine) { firstLine = false; continue; } // skip header
                bpeRanks.put(line, rank++);
            }

            // Build byte decoder (GPT-2 specific byte->unicode mapping)
            byteDecoder = buildByteDecoder();

            LOGGER.info("Tokenizer loaded! Vocab size: " + encoder.size());
            return true;

        } catch (Exception e) {
            LOGGER.error("Failed to load tokenizer: " + e.getMessage());
            return false;
        }
    }

    public long[] encode(String text) {
        // GPT-2 BPE encoding
        List<Integer> tokens = new ArrayList<>();
        
        // Split text into words with GPT-2 style space prefix
        String[] words = text.split("(?<= )|(?= )");
        
        for (String word : words) {
            // Convert to GPT-2 byte encoding
            String encoded = encodeWord(word);
            List<String> bpeTokens = bpe(encoded);
            for (String token : bpeTokens) {
                if (encoder.containsKey(token)) {
                    tokens.add(encoder.get(token));
                }
            }
        }

        return tokens.stream().mapToLong(Integer::longValue).toArray();
    }

    public String decode(long[] tokens) {
        StringBuilder sb = new StringBuilder();
        for (long token : tokens) {
            String piece = decoder.getOrDefault((int) token, "");
            // Convert GPT-2 unicode back to bytes
            for (char c : piece.toCharArray()) {
                if (byteDecoder.containsValue(c)) {
                    for (Map.Entry<Integer, Character> e : byteDecoder.entrySet()) {
                        if (e.getValue() == c) {
                            sb.append((char)(int)e.getKey());
                            break;
                        }
                    }
                } else {
                    sb.append(c);
                }
            }
        }
        return sb.toString();
    }

    private String encodeWord(String word) {
        // Map each character to GPT-2's byte-level unicode
        StringBuilder sb = new StringBuilder();
        byte[] bytes = word.getBytes(StandardCharsets.UTF_8);
        for (byte b : bytes) {
            int unsigned = b & 0xFF;
            Character mapped = byteDecoder.get(unsigned);
            if (mapped != null) {
                sb.append(mapped);
            }
        }
        return sb.toString();
    }

    private List<String> bpe(String token) {
        if (token.length() <= 1) return Collections.singletonList(token);

        List<String> word = new ArrayList<>();
        for (int i = 0; i < token.length(); i++) {
            word.add(String.valueOf(token.charAt(i)));
        }

        while (word.size() > 1) {
            // Find the pair with lowest BPE rank
            String bestPair = null;
            int bestRank = Integer.MAX_VALUE;

            for (int i = 0; i < word.size() - 1; i++) {
                String pair = word.get(i) + " " + word.get(i + 1);
                Integer rank = bpeRanks.get(pair);
                if (rank != null && rank < bestRank) {
                    bestRank = rank;
                    bestPair = pair;
                }
            }

            if (bestPair == null) break;

            // Merge the best pair
            String[] parts = bestPair.split(" ", 2);
            List<String> newWord = new ArrayList<>();
            int i = 0;
            while (i < word.size()) {
                if (i < word.size() - 1 && word.get(i).equals(parts[0]) && word.get(i+1).equals(parts[1])) {
                    newWord.add(parts[0] + parts[1]);
                    i += 2;
                } else {
                    newWord.add(word.get(i));
                    i++;
                }
            }
            word = newWord;
        }

        return word;
    }

    private Map<Integer, Character> buildByteDecoder() {
        // GPT-2's specific byte to unicode mapping
        Map<Integer, Character> map = new HashMap<>();
        int n = 0;
        for (int i = 0; i < 256; i++) {
            if ((i >= '!' && i <= '~') || (i >= '¡' && i <= '¬') || (i >= '®' && i <= 'ÿ')) {
                map.put(i, (char) i);
            } else {
                map.put(i, (char)(256 + n));
                n++;
            }
        }
        return map;
    }

    public int getEosToken() { return EOS_TOKEN; }
    public int getNewlineToken() { return NEWLINE_TOKEN; }
}
package net.kenji.advanced_ai_villagers.model;

import ai.onnxruntime.*;
import net.kenji.advanced_ai_villagers.AdvancedAiVillagers;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.nio.file.*;
import java.util.*;

public class VillagerAiModel {

    private static final Logger LOGGER = LogManager.getLogger("VillagerAI");
    private static OrtEnvironment env;
    private static OrtSession session;
    private static boolean loaded = false;
    private static VillagerTokenizer tokenizer = new VillagerTokenizer();

    public static final float TEMPERATURE_PRESET = 0.42F;
    public static final float TEMPERATURE_CHAT = 0.85F;
    public static final float REPETITION_PENALTY = 1.8F;
    public static final int PHRASE_REPEAT_LIMIT = 3;


    // Call this once when the mod starts up
    public static void load() {
        try {
            LOGGER.info("Loading Villager AI model...");

            env = OrtEnvironment.getEnvironment();

            // Extract both model files to a temp directory on disk
            // ONNX Runtime needs them as actual files, not streams
            Path tempDir = Files.createTempDirectory("villagerai");

            String[] modelFiles = {
                    "model.onnx",
                    "model.onnx.data"
            };

            for (String fileName : modelFiles) {
                InputStream stream = VillagerAiModel.class
                        .getResourceAsStream("/assets/" + AdvancedAiVillagers.MODID + "/model/" + fileName);

                if (stream == null) {
                    LOGGER.error("Could not find " + fileName + " in resources!");
                    return;
                }

                Path outPath = tempDir.resolve(fileName);
                Files.copy(stream, outPath, StandardCopyOption.REPLACE_EXISTING);
                LOGGER.info("Extracted " + fileName + " to " + outPath);
            }

            // Now load from the temp directory where both files exist together
            Path modelPath = tempDir.resolve("model.onnx");

            OrtSession.SessionOptions opts = new OrtSession.SessionOptions();
            session = env.createSession(modelPath.toString(), opts);

            loaded = true;
            if (!tokenizer.load()) {
                LOGGER.error("Tokenizer failed to load!");
                loaded = false;
            }

            LOGGER.info("Villager AI model loaded successfully!");

        } catch (Exception e) {
            LOGGER.error("Failed to load Villager AI model: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static String generate(String situation, float temperature, int maxTokens) {
        if (!loaded) return "";

        try {
            String prompt = "### Situation: " + situation + "\n### Villager:";

            // Tokenize the prompt manually using GPT-2's encoding
            // GPT-2 uses a simple space-based tokenization we can approximate
            long[] tokenIds = tokenizer.encode(prompt);
            int promptLength = tokenIds.length;
            // Run up to 30 generation steps (one token at a time)
            for (int i = 0; i < maxTokens; i++) {
                // Create input tensor
                long[][] inputArray = new long[1][tokenIds.length];
                inputArray[0] = tokenIds;

                OnnxTensor inputTensor = OnnxTensor.createTensor(env, inputArray);
                Map<String, OnnxTensor> inputs = new HashMap<>();
                inputs.put("input_ids", inputTensor);

                // Run inference
                OrtSession.Result result = session.run(inputs);
                float[][][] logits = (float[][][]) result.get(0).getValue();

                // Get the last token's logits and pick the highest scoring token
                float[] lastLogits = logits[0][tokenIds.length - 1];
                applyNoRepeatNgram(lastLogits, tokenIds, PHRASE_REPEAT_LIMIT);

                int nextToken = sampleWithTemperature(lastLogits, temperature, REPETITION_PENALTY, tokenIds);

                // Stop if we hit end of sequence token (50256 for GPT-2)
                if (nextToken == 50256) break;

                // Stop if we hit a newline token
                if (nextToken == 198) break;

                // Append new token
                tokenIds = appendToken(tokenIds, nextToken);

                inputTensor.close();
                result.close();
            }

            // Decode just the generated part (after the prompt)
            String response = tokenizer.decode(Arrays.copyOfRange(tokenIds, promptLength, tokenIds.length)).trim();

            // Strip anything from ### onwards (model trying to start next prompt)
            if (response.contains("###")) {
                response = response.substring(0, response.indexOf("###")).trim();
            }

            // Strip stage directions and anything after quotes/brackets
            response = response.replaceAll("\\(.*?\\)", "").trim();  // remove (anything in brackets)
            response = response.split("\"")[0].trim();               // cut at first quote
            response = response.split("\\*")[0].trim();              // cut at first asterisk

            // Clean up trailing punctuation oddities
            response = response.replaceAll("[\\s]+$", "").trim();

            return response;
        } catch (Exception e) {
            LOGGER.error("Generation failed: " + e.getMessage());
            return "";
        }
    }
    private static void applyNoRepeatNgram(float[] logits, long[] tokenIds, int ngramSize) {
        if (tokenIds.length < ngramSize) return;

        // Get the last (ngramSize-1) tokens as the current context
        long[] context = Arrays.copyOfRange(tokenIds, tokenIds.length - (ngramSize - 1), tokenIds.length);

        // Scan through all previous positions looking for this context
        for (int i = 0; i <= tokenIds.length - ngramSize; i++) {
            boolean match = true;
            for (int j = 0; j < ngramSize - 1; j++) {
                if (tokenIds[i + j] != context[j]) {
                    match = false;
                    break;
                }
            }
            // If we found the context before, ban the token that followed it
            if (match) {
                int bannedToken = (int) tokenIds[i + ngramSize - 1];
                if (bannedToken >= 0 && bannedToken < logits.length) {
                    logits[bannedToken] = Float.NEGATIVE_INFINITY;
                }
            }
        }
    }
    private static void applyTopP(double[] probs, float topP) {
        // Sort indices by probability descending
        Integer[] indices = new Integer[probs.length];
        for (int i = 0; i < indices.length; i++) indices[i] = i;
        Arrays.sort(indices, (a, b) -> Double.compare(probs[b], probs[a]));

        // Find cutoff point where cumulative prob exceeds topP
        double cumulative = 0;
        int cutoff = indices.length;
        for (int i = 0; i < indices.length; i++) {
            cumulative += probs[indices[i]];
            if (cumulative >= topP) {
                cutoff = i + 1;
                break;
            }
        }

        // Zero out everything below the cutoff
        for (int i = cutoff; i < indices.length; i++) {
            probs[indices[i]] = 0;
        }

        // Renormalize
        double sum = 0;
        for (double p : probs) sum += p;
        for (int i = 0; i < probs.length; i++) probs[i] /= sum;
    }

    private static long[] appendToken(long[] tokens, int newToken) {
        long[] result = new long[tokens.length + 1];
        System.arraycopy(tokens, 0, result, 0, tokens.length);
        result[tokens.length] = newToken;
        return result;
    }

    public static boolean isLoaded() {
        return loaded;
    }

    private static int sampleWithTemperature(float[] logits, float temperature, float repetitionPenalty, long[] generatedTokens) {
        // Apply repetition penalty
        float[] penalizedLogits = logits.clone();
        for (long token : generatedTokens) {
            int idx = (int) token;
            if (idx >= 0 && idx < penalizedLogits.length) {
                if (penalizedLogits[idx] > 0) {
                    penalizedLogits[idx] /= repetitionPenalty;
                } else {
                    penalizedLogits[idx] *= repetitionPenalty;
                }
            }
        }

        // Apply temperature scaling
        double[] scaled = new double[penalizedLogits.length];
        double maxVal = Float.NEGATIVE_INFINITY;
        for (float f : penalizedLogits) if (f > maxVal) maxVal = f;

        double sum = 0;
        for (int i = 0; i < penalizedLogits.length; i++) {
            scaled[i] = Math.exp((penalizedLogits[i] - maxVal) / temperature);
            sum += scaled[i];
        }

        // Normalize to probabilities first
        for (int i = 0; i < scaled.length; i++) scaled[i] /= sum;

        // THEN apply top-p (needs probabilities summing to 1)
        applyTopP(scaled, 0.9f);

        // top-p already renormalizes internally so sample directly
        double rand = Math.random();
        double cumulative = 0;
        for (int i = 0; i < scaled.length; i++) {
            cumulative += scaled[i];
            if (rand < cumulative) return i;
        }
        return scaled.length - 1;
    }
}

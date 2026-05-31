package net.kenji.advanced_ai_villagers.api.model;

import ai.onnxruntime.*;
import net.kenji.advanced_ai_villagers.AdvancedAiVillagers;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jline.utils.Log;

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
    public static final float TEMPERATURE_CHAT = 0.575F;
    public static final float REPETITION_PENALTY = 1.12F;
    public static final int PHRASE_REPEAT_LIMIT = 3;

    private static final Map<UUID, List<String>> recentResponses = new HashMap<>();
    private static final int RECENT_RESPONSE_LIMIT = 5;

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

    public static String generateResponse(String playerMessage, String context, float temperature, int maxTokens, UUID villagerUUID) {
        if (!loaded) return "";

        try {
            // Match the training format exactly
            String prompt = "<|context|> " + context + " <|player|> " + playerMessage + " <|villager|>";
            Log.info("FULL Ai Message and Prompt: " + prompt);
            long[] tokenIds = tokenizer.encode(prompt);
            int promptLength = tokenIds.length;

            for (int i = 0; i < maxTokens; i++) {
                long[][] inputArray = new long[1][tokenIds.length];
                inputArray[0] = tokenIds;

                OnnxTensor inputTensor = OnnxTensor.createTensor(env, inputArray);
                Map<String, OnnxTensor> inputs = new HashMap<>();
                inputs.put("input_ids", inputTensor);

                OrtSession.Result result = session.run(inputs);
                float[][][] logits = (float[][][]) result.get(0).getValue();

                float[] lastLogits = logits[0][tokenIds.length - 1];
                applyNoRepeatNgram(lastLogits, tokenIds, PHRASE_REPEAT_LIMIT);

                int nextToken = sampleWithTemperature(lastLogits, temperature, REPETITION_PENALTY, tokenIds);

                // Stop at EOS or endoftext token
                if (nextToken == 50256) break; // EOS endoftext
                if (nextToken == 50260) break; // PAD token
                if (nextToken == 198) break;

                tokenIds = appendToken(tokenIds, nextToken);
                inputTensor.close();
                result.close();
            }

            String response = tokenizer.decode(Arrays.copyOfRange(tokenIds, promptLength, tokenIds.length)).trim();

            // Strip the endoftext token if present
            response = response.replace("<|endoftext|>", "").trim();

            // Strip anything from special tokens onwards
            if (response.contains("<|")) {
                response = response.substring(0, response.indexOf("<|")).trim();
            }

            // Strip stage directions
            response = response.replaceAll("\\(.*?\\)", "").trim();
            response = response.split("\"")[0].trim();
            response = response.split("\\*")[0].trim();
            response = response.replaceAll("[\\s]+$", "").trim();

            if (!response.isEmpty() && !endsWithSentencePunctuation(response)) {
                int lastPunctuation = Math.max(
                        Math.max(response.lastIndexOf('.'), response.lastIndexOf('!')),
                        response.lastIndexOf('?')
                );
                if (lastPunctuation > response.length() / 2) {
                    response = response.substring(0, lastPunctuation + 1).trim();
                }
            }
            response = cleanVillagerResponse(response);
            response = response.replaceAll("[_\\s]{3,}", " ").trim();
            response = response.replaceAll("_+", "").trim();

            List<String> recent = recentResponses.getOrDefault(villagerUUID, new ArrayList<>());
            for (String prev : recent) {
                if (response.toLowerCase().contains("get inside") && prev.toLowerCase().contains("get inside")) {
                    // Force regenerate once with higher temperature
                    response = generateResponse(playerMessage, context, temperature + 0.2f, maxTokens, villagerUUID);
                }
            }
            recent.add(response);
            if (recent.size() > RECENT_RESPONSE_LIMIT) recent.remove(0);
            recentResponses.put(villagerUUID, recent);

            return response;

        } catch (Exception e) {
            LOGGER.error("Generation failed: " + e.getMessage());
            return "";
        }
    }

    private static String cleanVillagerResponse(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "Hmm hmm! Hello there.";
        }

        String cleaned = raw.trim()
                .replaceAll("^[Hh]mm+[.!? ]*", "")
                .replaceAll("^[Hh]rrm+[.!? ]*", "")
                .replace("<|endoftext|>", "")
                .replace("--", "")
                .replace("~", "")
                .replace("[", "")
                .replace("]", "")
                .replace("�", "")           // common unicode artifacts
                .replaceAll("[-=]{3,}", "") // remove long dashes
                .replaceAll("\\s+", " ")
                .replace("¯", "")
                .replace("|", "")
                .replace("/", "")
                .replace(">", "")
                .replace("<", "")
                .replace("\\", "")
                .replace("^", "")
                .replace("{", "")
                .replace("}", "")
                .replace("&", "")
                .replace("*", "")
                .replace("(", "")
                .replace(")", "")
                .replace("$", "")
                .replace("@", "")
                .replace("%", "")
                .replace("=", "")
                .replace("_", "")
                .trim();

        // Remove any trailing special characters
        cleaned = cleaned.replaceAll("[^\\w\\s.,!?'-]+$", "");

        return cleaned.isEmpty() ? "Hmm hmm! Hello there." : cleaned;
    }
    private static boolean endsWithSentencePunctuation(String text) {
        if (text.isEmpty()) return false;
        char last = text.charAt(text.length() - 1);
        return last == '.' || last == '!' || last == '?' || last == '~' || last == '-';
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

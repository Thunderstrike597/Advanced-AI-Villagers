package net.kenji.ai_talking_villagers.api.model;

import ai.onnxruntime.*;
import io.github.mightguy.spellcheck.symspell.api.DataHolder;
import io.github.mightguy.spellcheck.symspell.api.StringDistance;
import io.github.mightguy.spellcheck.symspell.common.SpellCheckSettings;
import net.kenji.ai_talking_villagers.AiTalkingVillagers;

import net.kenji.ai_talking_villagers.ConfigCommon;
import net.kenji.ai_voice_lib.api.OrtSessionEnvironment;
import net.kenji.ai_voice_lib.api.utils.OnnxLoadingUtils;
import net.kenji.ai_voice_lib.api.utils.SpellCorrectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jline.utils.Log;

import java.nio.file.*;
import java.util.*;

public class VillagerAiModel extends io.github.mightguy.spellcheck.symspell.impl.SymSpellCheck{

    private static final Logger LOGGER = LogManager.getLogger("VillagerAI");
    private static OrtSessionEnvironment ortSessionEnvironment;

    public static final float TEMPERATURE_PRESET = 0.42F;
    public static final float TEMPERATURE_CHAT = 0.575F;
    public static final float REPETITION_PENALTY = 1.12F;
    public static final int PHRASE_REPEAT_LIMIT = 3;

    private static final Map<UUID, List<String>> recentResponses = new HashMap<>();
    private static final int RECENT_RESPONSE_LIMIT = 5;

    private static final boolean USE_CONVERSATION_HISTORY = true;
    private static final Map<UUID, List<String[]>> conversationHistory = new HashMap<>();
    private static final int MAX_HISTORY_TURNS = 3;
    private static String MODEL_NAME = "model";

    private static String[] modelFilesSearchNames = {
            MODEL_NAME + ".onnx",
            MODEL_NAME + ".onnx.data",
            "merges.txt",
            "tokenizer.json",
            "vocab.json",
            "special_tokens_map.json",
    };
    // Separate history map for villager-to-villager exchanges
    private static final Map<UUID, List<String[]>> villagerConversationHistory = new HashMap<>();

    public VillagerAiModel(DataHolder dataHolder, StringDistance stringDistance, SpellCheckSettings spellCheckSettings) {
        super(dataHolder, stringDistance, spellCheckSettings);
    }

    // Call this once when the mod starts up
    public static void load() {
        Thread aiThread = new Thread(() -> {
        try {

            LOGGER.info("Loading Villager AI model...");

            String modelDir = "/assets/" + AiTalkingVillagers.MODID + "/model/" + ConfigCommon.MODEL_TYPE.get().getModelName() + "/";
            Path outputDir = Paths.get(
                    "ai_models/" + AiTalkingVillagers.MODID + "/model/" + ConfigCommon.MODEL_TYPE.get().getModelName() + "/"
            );
            ortSessionEnvironment = OnnxLoadingUtils.startOrtEnvSession(AiTalkingVillagers.class, modelDir, outputDir, MODEL_NAME, modelFilesSearchNames, true);
            if (ortSessionEnvironment != null) {
                LOGGER.info("Villager AI model loaded successfully!");
            } else {
                LOGGER.error("Failed to load Villager AI model: startOrtEnvSession returned null");
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load Villager AI model: " + e.getMessage());
            e.printStackTrace();
        }
        });
        aiThread.setDaemon(true);
        aiThread.start();
    }

    public static String generateResponse(String playerMessage, String context, float temperature, int maxTokens, UUID villagerUUID) {
        if (!isLoaded()) {
            Log.error("RESPONSE FAILED! MODEL NOT LOADED!!");
            return "";
        }

        try {
            String prompt;

            if (USE_CONVERSATION_HISTORY) {
                List<String[]> history = conversationHistory.getOrDefault(villagerUUID, new ArrayList<>());

                StringBuilder promptBuilder = new StringBuilder();
                promptBuilder.append("<|context|> ").append(context).append(" ");

                for (String[] turn : history) {
                    promptBuilder.append("<|player|> ").append(turn[0]).append(" ");
                    promptBuilder.append("<|villager|> ").append(turn[1]).append(" ");
                }

                promptBuilder.append("<|player|> ").append(playerMessage).append(" <|villager|>");
                prompt = promptBuilder.toString();
            } else {
                prompt = "<|context|> " + context + " <|player|> " + playerMessage + " <|villager|>";
            }

            LOGGER.info("Full prompt: " + prompt);
            long[] tokenIds = ortSessionEnvironment.tokenizer().encode(prompt);
            int promptLength = tokenIds.length;

            for (int i = 0; i < maxTokens; i++) {
                long[][] inputArray = new long[1][tokenIds.length];
                inputArray[0] = tokenIds;

                OnnxTensor inputTensor = OnnxTensor.createTensor(ortSessionEnvironment.env(), inputArray);
                Map<String, OnnxTensor> inputs = new HashMap<>();
                inputs.put("input_ids", inputTensor);

                OrtSession.Result result = ortSessionEnvironment.session().run(inputs);
                float[][][] logits = (float[][][]) result.get(0).getValue();

                float[] lastLogits = logits[0][tokenIds.length - 1];
                applyNoRepeatNgram(lastLogits, tokenIds, PHRASE_REPEAT_LIMIT);

                int nextToken = sampleWithTemperature(lastLogits, temperature, REPETITION_PENALTY, tokenIds);

                // Stop at EOS or endoftext token
                if (nextToken == 50256) break; // EOS endoftext
                if (nextToken == 50260) break; // PAD token

                tokenIds = appendToken(tokenIds, nextToken);
                inputTensor.close();
                result.close();
            }

            String response = ortSessionEnvironment.tokenizer().decode(Arrays.copyOfRange(tokenIds, promptLength, tokenIds.length)).trim();

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
            Log.info("Logging Response: " + response);
            String corrected = SpellCorrectionUtils.getCorrectionText(response);
            Log.info("Logging Corrected: " + corrected);

            saveResponse(villagerUUID, playerMessage, corrected);
            return corrected;
        } catch (Exception e) {
            LOGGER.error("Generation failed: " + e.getMessage());
            return "";
        }
    }

    public static String generateVillagerGreeting(String context, float temperature, int maxTokens, UUID villagerUUID) {
        if (!isLoaded()) {
            Log.error("GREETING FAILED! MODEL NOT LOADED!!");
            return "";
        }

        try {
            // Use a special prompt that signals the villager speaks first
            String prompt = "<|context|> " + context + " <|villager|>";

           // Log.info("Generating villager greeting with prompt: " + prompt);
            long[] tokenIds = ortSessionEnvironment.tokenizer().encode(prompt);
            int promptLength = tokenIds.length;

            for (int i = 0; i < maxTokens; i++) {
                long[][] inputArray = new long[1][tokenIds.length];
                inputArray[0] = tokenIds;

                OnnxTensor inputTensor = OnnxTensor.createTensor(ortSessionEnvironment.env(), inputArray);
                Map<String, OnnxTensor> inputs = new HashMap<>();
                inputs.put("input_ids", inputTensor);

                OrtSession.Result result = ortSessionEnvironment.session().run(inputs);
                float[][][] logits = (float[][][]) result.get(0).getValue();

                float[] lastLogits = logits[0][tokenIds.length - 1];
                applyNoRepeatNgram(lastLogits, tokenIds, PHRASE_REPEAT_LIMIT);

                int nextToken = sampleWithTemperature(lastLogits, temperature, REPETITION_PENALTY, tokenIds);

                if (nextToken == 50256) break;
                if (nextToken == 50260) break;
                if (nextToken == 198) break;

                tokenIds = appendToken(tokenIds, nextToken);
                inputTensor.close();
                result.close();
            }

            String response = ortSessionEnvironment.tokenizer().decode(Arrays.copyOfRange(tokenIds, promptLength, tokenIds.length)).trim();
            response = response.replace("<|endoftext|>", "").trim();
            if (response.contains("<|")) {
                response = response.substring(0, response.indexOf("<|")).trim();
            }

            response = cleanVillagerResponse(response);
            return response;

        } catch (Exception e) {
            LOGGER.error("Greeting generation failed: " + e.getMessage());
            return "";
        }
    }

    // Add this new method for villager-to-villager responses
    public static String generateVillagerReply(
            String otherVillagerMessage,
            String context,
            float temperature,
            int maxTokens,
            UUID speakingVillagerUUID,
            UUID listeningVillagerUUID) {

        if (!isLoaded()) {
            Log.error("VILLAGER REPLY FAILED! MODEL NOT LOADED!!");
            return "";
        }

        try {
            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append("<|context|> ").append(context).append(" ");

            // Append this villager's conversation history with the other villager
            // Key the history on a shared pair ID so both villagers share the same thread
            UUID pairId = getPairId(speakingVillagerUUID, listeningVillagerUUID);
            List<String[]> history = villagerConversationHistory.getOrDefault(pairId, new ArrayList<>());

            for (String[] turn : history) {
                promptBuilder.append("<|villager1|> ").append(turn[0]).append(" ");
                promptBuilder.append("<|villager2|> ").append(turn[1]).append(" ");
            }

            // The other villager just said something — we are villager2 responding
            promptBuilder.append("<|villager1|> ").append(otherVillagerMessage).append(" <|villager2|>");

            String prompt = promptBuilder.toString();
            LOGGER.info("Villager-to-villager prompt: " + prompt);

            long[] tokenIds = ortSessionEnvironment.tokenizer().encode(prompt);
            int promptLength = tokenIds.length;

            for (int i = 0; i < maxTokens; i++) {
                long[][] inputArray = new long[1][tokenIds.length];
                inputArray[0] = tokenIds;

                OnnxTensor inputTensor = OnnxTensor.createTensor(ortSessionEnvironment.env(), inputArray);
                Map<String, OnnxTensor> inputs = new HashMap<>();
                inputs.put("input_ids", inputTensor);

                OrtSession.Result result = ortSessionEnvironment.session().run(inputs);
                float[][][] logits = (float[][][]) result.get(0).getValue();

                float[] lastLogits = logits[0][tokenIds.length - 1];
                applyNoRepeatNgram(lastLogits, tokenIds, PHRASE_REPEAT_LIMIT);

                int nextToken = sampleWithTemperature(lastLogits, temperature, REPETITION_PENALTY, tokenIds);

                if (nextToken == 50256) break;
                if (nextToken == 50260) break;

                tokenIds = appendToken(tokenIds, nextToken);
                inputTensor.close();
                result.close();
            }

            String response = ortSessionEnvironment.tokenizer().decode(Arrays.copyOfRange(tokenIds, promptLength, tokenIds.length)).trim();
            response = response.replace("<|endoftext|>", "").trim();
            if (response.contains("<|")) {
                response = response.substring(0, response.indexOf("<|")).trim();
            }
            response = cleanVillagerResponse(response);

            // Save to the shared pair history
            saveVillagerExchange(pairId, otherVillagerMessage, response);
            return response;

        } catch (Exception e) {
            LOGGER.error("Villager reply generation failed: " + e.getMessage());
            return "";
        }
    }

    public static void clearVillagerConversation(UUID speakerUuid, UUID listerUuid){
        if(villagerConversationHistory.get(speakerUuid) != null){
            villagerConversationHistory.remove(speakerUuid);
        }
    }


    // Deterministic shared ID for any two villagers regardless of who speaks first
    private static UUID getPairId(UUID a, UUID b) {
        if (a.compareTo(b) < 0) {
            return UUID.nameUUIDFromBytes((a.toString() + b.toString()).getBytes());
        } else {
            return UUID.nameUUIDFromBytes((b.toString() + a.toString()).getBytes());
        }
    }


    private static void saveVillagerExchange(UUID pairId, String villager1Msg, String villager2Response) {
        List<String[]> hist = villagerConversationHistory.getOrDefault(pairId, new ArrayList<>());
        hist.add(new String[]{villager1Msg, villager2Response});
        if (hist.size() > MAX_HISTORY_TURNS) hist.remove(0);
        villagerConversationHistory.put(pairId, hist);
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

    private static void saveResponse(UUID villagerUUID, String playerMessage, String response){
        List<String[]> hist = conversationHistory.getOrDefault(villagerUUID, new ArrayList<>());
        hist.add(new String[]{playerMessage, response});
        if (hist.size() > MAX_HISTORY_TURNS) hist.remove(0); // trim oldest
        conversationHistory.put(villagerUUID, hist);
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
        return ortSessionEnvironment != null && ortSessionEnvironment.isEnvironmentWithTokenizerLoaded();
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

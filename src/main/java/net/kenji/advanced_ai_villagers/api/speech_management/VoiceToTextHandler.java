package net.kenji.advanced_ai_villagers.api.speech_management;

import ai.onnxruntime.*;
import net.kenji.advanced_ai_villagers.AdvancedAiVillagers;
import org.jline.utils.Log;

import java.io.InputStream;
import java.nio.file.*;
import java.util.*;

public class VoiceToTextHandler {
    private final Map<Integer, String> vocab = new HashMap<>();
    private static final float VAD_RMS_THRESHOLD = 0.01f; // tune this

    private OrtEnvironment env;
    private OrtSession session;

    private static final int SAMPLE_RATE = 16000;
    private static String MODEL_NAME = "model";

    public void init() {
        try {
            Log.info("=== Starting ONNX Speech Recognition (ORT) ===");

            Path modelDir = Paths.get(
                    "ai_models/" + AdvancedAiVillagers.MODID + "/model/wav2vec2"
            );
            Files.createDirectories(modelDir);

            String[] modelFiles = {MODEL_NAME +".onnx", MODEL_NAME + ".int8.onnx", "vocab.json", "tokenizer.json", "tokenizer_config.json"};

            for (String fileName : modelFiles) {
                Path outPath = modelDir.resolve(fileName);
                boolean needsExtract = !Files.exists(outPath) || Files.size(outPath) == 0;

                if (needsExtract) {
                    Log.info("Extracting " + fileName + "...");
                    try (InputStream in = getClass().getResourceAsStream(
                            "/assets/advanced_ai_villagers/model/wav2vec2/" + fileName)) {
                        if (in == null) {
                            Log.warn("Resource not found in jar: " + fileName);
                            continue;
                        }
                        Files.copy(in, outPath, StandardCopyOption.REPLACE_EXISTING);
                        long size = Files.size(outPath);
                        Log.info("Extracted " + fileName + " — " + size + " bytes");
                        if (size == 0) {
                            Log.warn("WARNING: " + fileName + " extracted as 0 bytes!");
                        }
                    }
                } else {
                    Log.info("Already extracted: " + fileName + " (" + Files.size(outPath) + " bytes)");
                }
            }

            Path modelFile = modelDir.resolve(MODEL_NAME + ".onnx");
            Log.info("Loading session from: " + modelFile.toAbsolutePath());

            env = OrtEnvironment.getEnvironment();
            OrtSession.SessionOptions options = new OrtSession.SessionOptions();
            options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);
            options.setIntraOpNumThreads(Runtime.getRuntime().availableProcessors());
            session = env.createSession(modelFile.toString(), options);

            Log.info("✅ ONNX Runtime model loaded successfully");
            loadVocab(modelDir);

        } catch (Exception e) {
            Log.error("❌ Failed to load ONNX model", e);
            e.printStackTrace();
        }
    }
    private void loadVocab(Path modelDir) {
        try {
            Path vocabPath = modelDir.resolve("vocab.json");

            String json = Files.readString(vocabPath);

            // VERY SIMPLE PARSER (works for HuggingFace vocab.json)
            // format: "token": index OR index: token depending on file

            com.google.gson.JsonObject obj =
                    com.google.gson.JsonParser.parseString(json).getAsJsonObject();

            for (String key : obj.keySet()) {
                String value = obj.get(key).getAsString();

                try {
                    int index = Integer.parseInt(key);
                    vocab.put(index, value);
                } catch (NumberFormatException e) {
                    try {
                        int index = Integer.parseInt(value);
                        vocab.put(index, key);
                    } catch (Exception ignored) {}
                }
                //Log.info("Example vocab entry 0: " + vocab.get(0));
            }

            Log.info("Loaded vocab size: " + vocab.size());

        } catch (Exception e) {
            Log.error("Failed to load vocab", e);
        }
    }
    public String transcribe(short[] rawPcm) {
        if (session == null || rawPcm == null || rawPcm.length == 0) return null;

        try {
            // 1. Convert to Floats (-1.0 to 1.0) and downsample
            float[] audio = new float[rawPcm.length];
            for (int i = 0; i < rawPcm.length; i++) {
                audio[i] = rawPcm[i] / 32768.0f;
            }
            audio = downsample(audio, 48000, 16000);

            // 2. EXTRACT FBANK FEATURES
            // 2. EXTRACT FBANK FEATURES (Returns raw [Frames][80])
            float[][] rawFbank = computeMelFbank(audio);
            int rawFrameCount = rawFbank.length;

// Handle Frame Stacking: Group every 7 frames into a 560-wide feature block
            int stackedFrameCount = rawFrameCount / 7;
            if (stackedFrameCount == 0) stackedFrameCount = 1; // Safeguard for very short audio clippings

            float[][] stackedFeatures = new float[stackedFrameCount][560];

            for (int i = 0; i < stackedFrameCount; i++) {
                for (int j = 0; j < 7; j++) {
                    int originalFrameIdx = (i * 7) + j;

                    // Handle edge boundary protection if audio cuts short
                    if (originalFrameIdx >= rawFrameCount) {
                        originalFrameIdx = rawFrameCount - 1;
                    }

                    // Copy the 80 channels into the respective segment of the 560 block row
                    System.arraycopy(rawFbank[originalFrameIdx], 0, stackedFeatures[i], j * 80, 80);
                }
            }

        // 3. Reshape stacked grid to fit the 3D Tensor shape: [1, Stacked_Frames, 560]
            float[][][] feature3D = new float[1][stackedFrameCount][560];
            feature3D[0] = stackedFeatures;

        // 4. Create the multi-input parameters required by the graph (Fixed to 32-bit Integers)
            OnnxTensor tensorX = OnnxTensor.createTensor(env, feature3D);

        // IMPORTANT: x_length must reflect the STACKED count now, not raw frames!
            int[] lensData = new int[]{ stackedFrameCount };
            OnnxTensor tensorXLens = OnnxTensor.createTensor(env, lensData);

            int[] langData = new int[]{ 2 };
            OnnxTensor tensorLang = OnnxTensor.createTensor(env, langData);

            int[] normData = new int[]{ 1 };
            OnnxTensor tensorNorm = OnnxTensor.createTensor(env, normData);

        // Map inputs to the exact entry port identifiers
            Map<String, OnnxTensor> inputs = new HashMap<>();
            inputs.put("x", tensorX);
            inputs.put("x_length", tensorXLens);
            inputs.put("language", tensorLang);
            inputs.put("text_norm", tensorNorm);

            try (OrtSession.Result result = session.run(inputs)) {
                if (result == null || result.size() == 0) {
                    Log.warn("ONNX session executed successfully but returned an empty result mapping.");
                    return "";
                }

                Object outputValue = result.get(0).getValue();
                StringBuilder decodedText = new StringBuilder();

                //Log.info("SenseVoice output class signature detected: " + outputValue.getClass().getName());

                // SCENARIO E: Handle 3D Float Logits [[[F (This is your model's native format!)
                if (outputValue instanceof float[][][] logits) {
                    int lastIndex = -1;

                    // Loop through each time frame in the first batch item
                    for (float[] timeStep : logits[0]) {
                        int bestIndex = 0;
                        float maxLogit = timeStep[0];

                        // ArgMax calculation: Find the index with the highest probability value
                        for (int i = 1; i < timeStep.length; i++) {
                            if (timeStep[i] > maxLogit) {
                                maxLogit = timeStep[i];
                                bestIndex = i;
                            }
                        }

                        // Standard CTC deduplication logic: Skip repeated frames and blank/padding indices
                        if (bestIndex != lastIndex && bestIndex != 0) {
                            appendToken(bestIndex, decodedText);
                        }
                        lastIndex = bestIndex;
                    }
                    return cleanDialogue(decodedText.toString().trim());
                }

                // --- Keep your existing long/int array fallback scenarios below just in case ---
                else if (outputValue instanceof long[][] tokenIds2D) {
                    for (long id : tokenIds2D[0]) appendToken(id, decodedText);
                    return cleanDialogue(decodedText.toString().trim());
                }
                else if (outputValue instanceof int[][] tokenIds2D) {
                    for (int id : tokenIds2D[0]) appendToken(id, decodedText);
                    return cleanDialogue(decodedText.toString().trim());
                }
                else if (outputValue instanceof long[] tokenIds1D) {
                    for (long id : tokenIds1D) appendToken(id, decodedText);
                    return cleanDialogue(decodedText.toString().trim());
                }
                else if (outputValue instanceof int[] tokenIds1D) {
                    for (int id : tokenIds1D) appendToken(id, decodedText);
                    return cleanDialogue(decodedText.toString().trim());
                }

                Log.warn("Unhandled output structure type encountered. Unable to map tokens safely.");
                return "";

            } finally {
                // Free explicit system references safely
                tensorX.close();
                tensorXLens.close();
                tensorLang.close();
                tensorNorm.close();
            }
        } catch (Exception e) {
            Log.error("SenseVoice Transcription failure: " + e.getMessage(), e);
        }
        return null;
    }


    private void appendToken(long id, StringBuilder decodedText) {
        String word = vocab.get((int) id);
        if (word == null) return;

        // 1. Catch and convert ALL types of special SenseVoice space characters
        if (word.equals("▁") || word.equals("⁇") || word.equals("|_|") || word.equals(" ") || word.equals("⦅space⦆")) {
            decodedText.append(" ");
            return;
        }

        // 2. Filter out system metadata event tags
        if (word.startsWith("<") || word.startsWith("[")) {
            return;
        }

        // 3. Strict Language Guard: Skip Chinese/Japanese characters if they bleed in
        // This stops the model from switching to random Asian symbols during mic noise/breathing
        if (word.matches(".*[\\u4e00-\\u9fa5\\u3040-\\u309f\\u30a0-\\u30ff].*")) {
            return;
        }

        // 4. Otherwise, safely append the clean English text token piece
        decodedText.append(word);
    }
    /**
     * Placeholder method for Feature Extraction.
     * To make SenseVoice function, you must pass audio through a Log-Mel Fbank algorithm.
     * You can write your own or use standard libraries like JAudioLibs to output an [audio_frames][80] grid.
     */
    private float[][] computeMelFbank(float[] audio) {
        // Passes the downsampled 16kHz audio array through the math processor
        return AudioFeatureExtractor.computeMelFbank(audio);
    }

    private float[] downsample(float[] input, int from, int to) {
        int step = from / to;
        float[] output = new float[input.length / step];
        for (int i = 0; i < output.length; i++) {
            output[i] = input[i * step];
        }
        return output;
    }



    private boolean isSpeech(short[] pcm) {
        // Calculate RMS energy of the signal
        double sum = 0;
        for (short s : pcm) {
            float f = s / 32768.0f;
            sum += f * f;
        }
        float rms = (float) Math.sqrt(sum / pcm.length);
        //Log.info("VAD RMS: " + rms + " (threshold: " + VAD_RMS_THRESHOLD + ")");
        return rms >= VAD_RMS_THRESHOLD;
    }

    private static String cleanDialogue(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }

        String cleaned = raw.trim()
                .replace("--", "")
                .replace("~", "")
                .replace("[", "")
                .replace("]", "")
                .replace("�", "")           // common unicode artifacts
                .replaceAll("[-=]{3,}", "") // remove long dashes
                .replaceAll("\\s+", " ")
                .replace("¯", " ")
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
                .replace("_", " ")
                .replace("▁", " ")
                .trim();

        return cleaned;
    }

    private float[] normalize(float[] audio) {
        // Zero-mean, unit-variance normalization
        double mean = 0;
        for (float v : audio) mean += v;
        mean /= audio.length;

        double variance = 0;
        for (float v : audio) variance += (v - mean) * (v - mean);
        variance /= audio.length;
        double std = Math.sqrt(variance + 1e-7);

        float[] out = new float[audio.length];
        for (int i = 0; i < audio.length; i++) {
            out[i] = (float) ((audio[i] - mean) / std);
        }
        return out;
    }

    /**
     * VERY SIMPLE greedy CTC decoder (placeholder version)
     * You can improve this later with vocab mapping
     */
    private String decodeCTC(float[][][] logits) {
        StringBuilder text = new StringBuilder();

        int lastIndex = -1;

        for (float[] timeStep : logits[0]) {
            int bestIndex = argMax(timeStep);

            // skip blanks + repeats
            if (bestIndex != lastIndex && bestIndex != 0) {
                String token = vocab.get(bestIndex);

                if (token != null &&
                        !token.equals("<pad>") &&
                        !token.equals("<blank>")) {

                    if (token.equals("|")) {
                        text.append(" ");
                    } else {
                        text.append(token);
                    }
                }
            }

            lastIndex = bestIndex;
        }

        return text.toString().trim();
    }

    private int argMax(float[] arr) {
        int idx = 0;
        float max = arr[0];

        for (int i = 1; i < arr.length; i++) {
            if (arr[i] > max) {
                max = arr[i];
                idx = i;
            }
        }
        return idx;
    }

    public void close() {
        try {
            if (session != null) session.close();
            if (env != null) env.close();
        } catch (Exception e) {
            Log.error("Error closing ONNX session", e);
        }
    }
}
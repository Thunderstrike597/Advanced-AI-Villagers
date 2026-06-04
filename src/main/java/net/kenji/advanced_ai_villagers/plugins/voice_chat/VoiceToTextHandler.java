package net.kenji.advanced_ai_villagers.plugins.voice_chat;

import ai.onnxruntime.*;
import net.kenji.advanced_ai_villagers.AdvancedAiVillagers;
import org.jline.utils.Log;

import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.file.*;
import java.util.*;

public class VoiceToTextHandler {
    private final Map<Integer, String> vocab = new HashMap<>();
    private static final float VAD_RMS_THRESHOLD = 0.01f; // tune this

    private OrtEnvironment env;
    private OrtSession session;

    private static final int SAMPLE_RATE = 16000;

    public void init() {
        try {
            Log.info("=== Starting ONNX Speech Recognition (ORT) ===");

            Path modelDir = Paths.get(
                    "ai_models/" + AdvancedAiVillagers.MODID + "/model/wav2vec2"
            );
            Files.createDirectories(modelDir);

            String[] modelFiles = {"model.onnx", "model.onnx.data", "vocab.json"};

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

            Path modelFile = modelDir.resolve("model.onnx");
            Log.info("Loading session from: " + modelFile.toAbsolutePath());

            env = OrtEnvironment.getEnvironment();
            OrtSession.SessionOptions options = new OrtSession.SessionOptions();
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
                Log.info("Example vocab entry 0: " + vocab.get(0));
            }

            Log.info("Loaded vocab size: " + vocab.size());

        } catch (Exception e) {
            Log.error("Failed to load vocab", e);
        }
    }
    public String transcribe(short[] rawPcm) {
        if (session == null) return null;
        if (rawPcm == null || rawPcm.length == 0) return null;

        // Gate: reject audio that's too quiet to be real speech
        if (!isSpeech(rawPcm)) {
            Log.info("VAD rejected — audio too quiet (background noise/breathing)");
            return null;
        }
        try {
            // 1. Convert PCM → float
            float[] audio = new float[rawPcm.length];
            for (int i = 0; i < rawPcm.length; i++) {
                audio[i] = rawPcm[i] / 32768.0f;
            }

            // 2. Downsample from 48000Hz → 16000Hz (take every 3rd sample)
            audio = downsample(audio, 48000, 16000);
            Log.info("After downsample: " + audio.length + " samples (" + (audio.length / 16000f) + "s)");

            // 3. Normalize (Wav2Vec2 feature extractor does this internally during training)
            audio = normalize(audio);

            // 4. Build input tensor [1, samples]
            float[][] input = new float[1][audio.length];
            input[0] = audio;

            OnnxTensor inputTensor = OnnxTensor.createTensor(env, input);
            Map<String, OnnxTensor> inputs = new HashMap<>();
            inputs.put("input_values", inputTensor);

            // 5. Run inference
            OrtSession.Result result = session.run(inputs);
            Object output = result.get(0).getValue();

            if (output instanceof float[][][] logits) {
                Log.info("Logits shape: [" + logits.length + "][" + logits[0].length + "][" + logits[0][0].length + "]");
                String decoded = decodeCTC(logits);
                Log.info("Decoded result: '" + decoded + "'");
                inputTensor.close();
                result.close();
                return decoded;
            }

            Log.warn("Unexpected output type: " + output.getClass().getName());
            return null;

        } catch (Exception e) {
            Log.error("transcribe() exception: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }


    private boolean isSpeech(short[] pcm) {
        // Calculate RMS energy of the signal
        double sum = 0;
        for (short s : pcm) {
            float f = s / 32768.0f;
            sum += f * f;
        }
        float rms = (float) Math.sqrt(sum / pcm.length);
        Log.info("VAD RMS: " + rms + " (threshold: " + VAD_RMS_THRESHOLD + ")");
        return rms >= VAD_RMS_THRESHOLD;
    }

    private float[] downsample(float[] input, int fromRate, int toRate) {
        // Simple decimation — take every Nth sample
        // Good enough for speech; proper resampling would use a low-pass filter first
        int ratio = fromRate / toRate;
        float[] output = new float[input.length / ratio];
        for (int i = 0; i < output.length; i++) {
            output[i] = input[i * ratio];
        }
        return output;
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
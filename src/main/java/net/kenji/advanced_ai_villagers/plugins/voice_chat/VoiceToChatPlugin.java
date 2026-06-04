package net.kenji.advanced_ai_villagers.plugins.voice_chat;

import ai.djl.modality.audio.translator.SpeechRecognitionTranslator;
import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.ClientSoundEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;

import de.maxhenkel.voicechat.api.events.MicrophoneMuteEvent;
import net.kenji.advanced_ai_villagers.AdvancedAiVillagers;
import net.kenji.advanced_ai_villagers.api.SpeechManager;
import net.kenji.advanced_ai_villagers.network.ModPacketHandler;
import net.kenji.advanced_ai_villagers.network.ServerVoiceMessagePacket;
import net.minecraft.client.Minecraft;
import org.jline.utils.Log;

import java.util.ArrayList;
import java.util.List;

@ForgeVoicechatPlugin
public class VoiceToChatPlugin implements VoicechatPlugin {

    private final List<short[]> audioBuffer = new ArrayList<>();
    private final VoiceToTextHandler handler = new VoiceToTextHandler();
    private boolean enabled = true;

    // Silence detection
    private static final long SILENCE_TIMEOUT_MS = 500;
    private volatile long lastFrameTime = 0;
    private volatile boolean flushScheduled = false;

    @Override
    public String getPluginId() { return AdvancedAiVillagers.MODID; }

    @Override
    public void initialize(VoicechatApi api) { handler.init(); }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(ClientSoundEvent.class, this::onClientSound);
    }

    private synchronized void onClientSound(ClientSoundEvent event) {
        if (!enabled) return;
        short[] rawAudio = event.getRawAudio();
        if (rawAudio == null || rawAudio.length == 0) return;

        audioBuffer.add(rawAudio.clone());
        lastFrameTime = System.currentTimeMillis();

        // Schedule a flush check if one isn't already pending
        if (!flushScheduled) {
            flushScheduled = true;
            Thread t = new Thread(this::waitAndFlush);
            t.setDaemon(true);
            t.start();
        }
    }

    private void waitAndFlush() {
        try {
            while (true) {
                Thread.sleep(100);
                long silenceMs = System.currentTimeMillis() - lastFrameTime;
                if (silenceMs >= SILENCE_TIMEOUT_MS) break;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        flush();
    }

    private synchronized void flush() {
        flushScheduled = false;
        if (audioBuffer.isEmpty()) return;

        short[] fullAudio = flattenAudio(audioBuffer);
        audioBuffer.clear();
        if(fullAudio.length < 16000) return;
        short[] finalAudio = fullAudio;
        Thread t = new Thread(() -> {
            String text = handler.transcribe(finalAudio);
            if (text != null && !text.isBlank()) {
                Log.info("✅ Speech recognized: " + text);
                Minecraft.getInstance().execute(() -> {
                    if (Minecraft.getInstance().player != null) {
                        ModPacketHandler.sendToServer(new ServerVoiceMessagePacket(text));
                    }
                });
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private short[] flattenAudio(List<short[]> frames) {
        int total = frames.stream().mapToInt(f -> f.length).sum();
        short[] out = new short[total];
        int pos = 0;
        for (short[] frame : frames) {
            System.arraycopy(frame, 0, out, pos, frame.length);
            pos += frame.length;
        }
        return out;
    }

    public void toggle() { enabled = !enabled; }
}
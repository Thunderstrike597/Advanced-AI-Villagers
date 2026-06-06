package net.kenji.advanced_ai_villagers.plugins.voice_chat;

import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.ClientSoundEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;

import io.github.mightguy.spellcheck.symspell.exception.SpellCheckException;
import net.kenji.advanced_ai_villagers.AiTalkingVillagers;
import net.kenji.advanced_ai_villagers.api.manager.SpeechManager;
import net.kenji.advanced_ai_villagers.network.ModPacketHandler;
import net.kenji.advanced_ai_villagers.network.ServerVoiceMessagePacket;
import net.kenji.ai_voice_lib.api.speech_management.VoiceToTextHandler;
import net.kenji.ai_voice_lib.api.utils.SpellCorrectionUtils;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

@ForgeVoicechatPlugin
public class VoiceToChatPlugin implements VoicechatPlugin {

    private final List<short[]> audioBuffer = new ArrayList<>();
    private final VoiceToTextHandler handler = new VoiceToTextHandler();
    private boolean enabled = true;

    // Silence detection
    private static final long SILENCE_TIMEOUT_MS = 300;
    private volatile long lastFrameTime = 0;
    private volatile boolean flushScheduled = false;

    @Override
    public String getPluginId() { return AiTalkingVillagers.MODID; }

    @Override
    public void initialize(VoicechatApi api) {
        handler.init();
    }

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
        Thread t = new Thread(() -> {
            String text = handler.transcribe(fullAudio);
            //Log.info("Transcribed Audio: " + text);
            if (text != null && !text.isBlank()) {
                //Log.info("✅ Speech recognized: " + text);
                Minecraft.getInstance().execute(() -> {
                    if (Minecraft.getInstance().player != null) {
                        try {
                            String corrected = SpellCorrectionUtils.getCorrectionText(text);
                            ModPacketHandler.sendToServer(new ServerVoiceMessagePacket(corrected));
                            Minecraft.getInstance().player.getPersistentData().putString(SpeechManager.PLAYER_SPEECH_TAG, corrected);
                        } catch (SpellCheckException e) {
                            throw new RuntimeException(e);
                        }

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
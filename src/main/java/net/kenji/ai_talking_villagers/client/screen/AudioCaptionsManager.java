package net.kenji.ai_talking_villagers.client.screen;

import net.kenji.ai_talking_villagers.AiTalkingVillagers;
import net.kenji.ai_talking_villagers.api.manager.SpeechManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AiTalkingVillagers.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class AudioCaptionsManager {



    @SubscribeEvent
    public static void renderCaptions(RenderGuiOverlayEvent event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player == null) return;

        String captionText = player.getPersistentData().getString(SpeechManager.PLAYER_SPEECH_TAG);
        if (captionText.isEmpty()) return;

        var font = mc.font;
        var guiGraphics = event.getGuiGraphics();

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int maxWidth = Math.min(300, screenWidth - 40);
        int x = (screenWidth - maxWidth) / 2 + 20;
        int y = screenHeight - 60;

        // Split into wrapped lines
        var lines = font.split(Component.literal(captionText), maxWidth);

        // Draw background box
        int padding = 4;
        int lineHeight = font.lineHeight + 2;
        int totalHeight = lines.size() * lineHeight;

        // Draw each wrapped line
        for (int i = 0; i < lines.size(); i++) {
            guiGraphics.drawString(font, lines.get(i), x, y + (i * lineHeight), 0xFFFFFF, true);
        }
    }


    @SubscribeEvent
    public static void onClientTick(TickEvent.PlayerTickEvent event){
        if(event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if(!player.level().isClientSide()) return;
        String captionText = player.getPersistentData().getString(SpeechManager.PLAYER_SPEECH_TAG);
        if (captionText.isEmpty()) return;

        int counter = SpeechManager.captionCounterMap.getOrDefault(player.getUUID(), SpeechManager.CAPTION_COUNTER_MAX);
        if(counter > 0) {
            counter--;
            SpeechManager.captionCounterMap.put(player.getUUID(), counter);
        }
        else{
            player.getPersistentData().remove(SpeechManager.PLAYER_SPEECH_TAG);
            SpeechManager.captionCounterMap.remove(player.getUUID());
        }
    }
}

package net.kenji.ai_talking_villagers.events;

import net.kenji.ai_talking_villagers.AiTalkingVillagers;
import net.kenji.ai_talking_villagers.client.screen.AudioCaptionsManager;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AiTalkingVillagers.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent event) {
        AudioCaptionsManager.renderCaptions(event);
    }
}

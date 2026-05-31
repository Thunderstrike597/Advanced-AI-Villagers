package net.kenji.advanced_ai_villagers.events;

import net.kenji.advanced_ai_villagers.AdvancedAiVillagers;
import net.kenji.advanced_ai_villagers.api.SpeechManager;
import net.kenji.advanced_ai_villagers.api.context.ContextManager;
import net.kenji.advanced_ai_villagers.api.model.VillagerAiModel;
import net.kenji.advanced_ai_villagers.client.render.TextBubbleRenderer;
import net.kenji.advanced_ai_villagers.network.ClientTagSyncPacket;
import net.kenji.advanced_ai_villagers.network.ModPacketHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jline.utils.Log;

import java.util.Comparator;

@Mod.EventBusSubscriber(modid = AdvancedAiVillagers.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerChatEvents {

    @SubscribeEvent
    public static void onPlayerChat(ServerChatEvent event) {
        String message = event.getMessage().getString();

        ServerPlayer player =  event.getPlayer();
        Villager nearest = findNearestVillager(player, 10); // within 10 blocks




        if (nearest == null) return; // no villager nearby, ignore

        final String[] situation = {"Player says to villager: " + message};


        Thread aiThread = new Thread(() -> {
            String context = ContextManager.getVillagerContext(nearest);
            // context already returns "Loc=Village, Time=Day, Shelter=Outside" etc
            // strip the "Context: " prefix since the prompt template handles that
            context = context.replace("Context: ", "");

            String response = VillagerAiModel.generateResponse(message, context, VillagerAiModel.TEMPERATURE_CHAT, 25, nearest.getUUID());
            Log.info("Response: " + response);
            // Replace name placeholder
            response = response.replace("VILLAGER_NAME", nearest.getName().getString());
            response = response.replace("VILLAGERNAME", nearest.getName().getString());
            response = response.replace("VILLAGER NAME", nearest.getName().getString());

            if (!response.isEmpty()) {
                String finalResponse = response;
                nearest.getServer().execute(() -> {
                    SpeechManager.addVillagerSpeech(nearest, player, finalResponse);
                });
            }
        });
        aiThread.setDaemon(true);
        aiThread.start();
    }
    
    private static Villager findNearestVillager(ServerPlayer player, double range) {
        return player.level().getEntitiesOfClass(
            Villager.class,
            player.getBoundingBox().inflate(range)
        ).stream()
            .min(Comparator.comparingDouble(v -> v.distanceTo(player)))
            .orElse(null);
    }
}
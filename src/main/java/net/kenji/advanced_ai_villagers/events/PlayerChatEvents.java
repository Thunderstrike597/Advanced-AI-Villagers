package net.kenji.advanced_ai_villagers.events;

import net.kenji.advanced_ai_villagers.AdvancedAiVillagers;
import net.kenji.advanced_ai_villagers.model.VillagerAiModel;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraftforge.client.event.ClientChatEvent;
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
        
        String situation = "Player says to villager: " + message;
        Log.info("AI Situation: " + situation);
        
        Thread aiThread = new Thread(() -> {
            String response = VillagerAiModel.generate(situation, VillagerAiModel.TEMPERATURE_CHAT, 20);
            if (!response.isEmpty()) {
                nearest.getServer().execute(() -> {
                    player.sendSystemMessage(
                        Component.literal("[" + nearest.getName().getString() + "]: " + response)
                    );
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
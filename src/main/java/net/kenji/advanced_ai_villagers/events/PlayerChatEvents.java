package net.kenji.advanced_ai_villagers.events;

import net.kenji.advanced_ai_villagers.AdvancedAiVillagers;
import net.kenji.advanced_ai_villagers.api.SpeechManager;
import net.kenji.advanced_ai_villagers.api.context.ContextManager;
import net.kenji.advanced_ai_villagers.api.model.VillagerAiModel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jline.utils.Log;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Mod.EventBusSubscriber(modid = AdvancedAiVillagers.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerChatEvents {

    @SubscribeEvent
    public static void onPlayerChat(ServerChatEvent event) {
        String message = event.getMessage().getString();

        ServerPlayer player =  event.getPlayer();
        Villager nearest = findNearestVillager(player, 10); // within 10 blocks
        List<Villager> villagerGroup = findVillagerGroup(player, 5, 2.5F, nearest);

        if (nearest == null && villagerGroup.isEmpty()) return; // no villager nearby, ignore

        final String[] situation = {"Player says to villager: " + message};


        Thread aiThread = new Thread(() -> {
            villagerGroup.forEach((villager) ->{
            String context = ContextManager.getPlayerChatContext(villager, player);

            String response = VillagerAiModel.generateResponse(message, context, VillagerAiModel.TEMPERATURE_CHAT, 25, villager.getUUID());
            Log.info("Response: " + response);
            // Replace name placeholder
            response = response.replace("VILLAGER_NAME", villager.getName().getString());
            response = response.replace("VILLAGERNAME", villager.getName().getString());
            response = response.replace("VILLAGER NAME", villager.getName().getString());

            if (!response.isEmpty()) {
                String finalResponse = response;
                villager.getServer().execute(() -> {
                    SpeechManager.addVillagerSpeech(villager, player, finalResponse);
                });
            }
            });
        });
        aiThread.setDaemon(true);
        aiThread.start();
    }

    private static List<Villager> findVillagerGroup(ServerPlayer player, double range,double nearbyRange, Villager villagerToIgnore) {
        List<Villager> villagers = new ArrayList<>(player.level().getEntitiesOfClass(
                Villager.class,
                player.getBoundingBox().inflate(range)
        ).stream().toList());

        villagers.add(villagerToIgnore);

        List<Villager> finalVillagers = new ArrayList<>();
        List<Villager> nearbyVillagers = villagerToIgnore.level().getEntitiesOfClass(
                Villager.class,
                        villagerToIgnore.getBoundingBox().inflate(nearbyRange)
        );
        nearbyVillagers.forEach(villager ->{
            if(player.position().distanceTo(villager.position()) <= range)
                finalVillagers.add(villager);
        });
        if(!finalVillagers.contains(villagerToIgnore))
            finalVillagers.add(villagerToIgnore);

        return finalVillagers;
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
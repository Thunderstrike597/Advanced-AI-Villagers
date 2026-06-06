package net.kenji.advanced_ai_villagers.api.handler;

import net.kenji.advanced_ai_villagers.api.manager.SpeechManager;
import net.kenji.advanced_ai_villagers.api.context.ContextManager;
import net.kenji.advanced_ai_villagers.api.model.VillagerAiModel;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import org.jline.utils.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VillagerHurtHandler {

    // Cooldown so the villager doesn't spam every single hit
    private static final Map<UUID, Long> lastSpokenTime = new HashMap<>();
    private static final long COOLDOWN_MS = 4000; // 4 seconds between messages


    public static void managerVillagerAttacked(LivingAttackEvent event){
        // Check if the thing being attacked is a villager
        if (!(event.getEntity() instanceof Villager villager)) return;

        // Check if the attacker is a zombie
        if (!(event.getSource().getEntity() instanceof Zombie)) return;

        // Check cooldown so it doesn't fire every tick
        UUID villagerID = villager.getUUID();
        long now = System.currentTimeMillis();
        if (lastSpokenTime.containsKey(villagerID)) {
            if (now - lastSpokenTime.get(villagerID) < COOLDOWN_MS) return;
        }
        lastSpokenTime.put(villagerID, now);

        // Build the situation string — this is what gets passed to the model

        // Run inference off the main thread so it doesn't freeze the game
        SpeechManager.aiThreadPool.execute(() -> {
            Log.info("Model loaded state: " + VillagerAiModel.isLoaded());
            String context = ContextManager.getVillagerCombatContext(villager);
            String response = VillagerAiModel.generateResponse(buildSituation(villager), context, VillagerAiModel.TEMPERATURE_PRESET, 15, villagerID);
            Log.info("Logging Villager Hurt Response: " + response);

            if (!response.isEmpty()) {
                // Send back to main thread to display
                villager.getServer().execute(() -> {
                    // Displays as chat message for now
                    // We'll replace this with a speech bubble later
                    villager.level().players().forEach(player ->{
                                SpeechManager.addVillagerSpeech(villager, player, response);

                            }
                    );
                });
            }
        });
    }


    private static String buildSituation(Villager villager) {
        float health = villager.getHealth();
        float maxHealth = villager.getMaxHealth();
        float healthPercent = (health / maxHealth) * 100;

        // Pass different situation strings based on health
        // These match your training data exactly
        if (healthPercent < 25) {
            return "A zombie is attacking the villager low on health";
        } else {
            return "A zombie is attacking the villager";
        }
    }
}
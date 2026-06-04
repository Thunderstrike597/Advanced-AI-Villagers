package net.kenji.advanced_ai_villagers.api;

import com.sun.jna.Memory;
import net.kenji.advanced_ai_villagers.AdvancedAiVillagers;
import net.kenji.advanced_ai_villagers.api.context.villager_info.PersonalityContext;
import net.kenji.advanced_ai_villagers.api.context.villager_info.RecentEventContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.entity.raid.Raids;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber(modid = AdvancedAiVillagers.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RecentEventManager {
    public static Map<UUID, Integer> recentEventTickCount = new HashMap<>();
    public static int MAX_RECENT_EVENT_TIME = 24000 * 7;

    @SubscribeEvent
    public static void onVillagerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Villager villager))
            return;
        List<Villager> nearbyVillagers = findNearbyVillagers(villager, 80, 40);

        for (Villager nearby : nearbyVillagers) {
            if (nearby != villager)
                nearby.getPersistentData().putString(SpeechManager.RECENT_EVENT_TAG, RecentEventContext.VILLAGER_DEATH.getTagName());
        }
    }
    @SubscribeEvent
    public static void onRaidMobDeath(LivingDeathEvent event) {
        // We only care about the server side
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        ServerLevel serverLevel = (ServerLevel) event.getEntity().level();

        // Ensure the entity killed is part of a raid
        if (event.getEntity().getTags().contains("Raider") || event.getEntity() instanceof Raider) {

            // Get the raid at the entity's position
            Raid raid = serverLevel.getRaidAt(event.getEntity().blockPosition());

            if (raid != null) {
                // Check if the raid is officially over
                if (raid.isStopped()) {
                    if (raid.isOver()) {
                        Villager villager = findNearestVillager(event.getEntity(), 40);

                        List<Villager> nearbyVillagers = findNearbyVillagers(villager, 80, 40);
                        if(!nearbyVillagers.contains(villager))
                            nearbyVillagers.add(villager);
                        for (Villager nearby : nearbyVillagers) {
                            nearby.getPersistentData().putString(SpeechManager.RECENT_EVENT_TAG, RecentEventContext.RAID_SURVIVED.getTagName());
                        }
                    }
                }
            }
        }
    }
    @SubscribeEvent
    public static void onVillagerTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Villager villager)) return;

        manageVillagerDeathCount(villager);
    }


    private static void manageVillagerDeathCount(Villager villager) {
        for (RecentEventContext context : RecentEventContext.values()) {
            if (!villager.getPersistentData().getString(SpeechManager.RECENT_EVENT_TAG).equals(context.getTagName()))
                continue;

            recentEventTickCount.putIfAbsent(villager.getUUID(), MAX_RECENT_EVENT_TIME);
            int count = recentEventTickCount.get(villager.getUUID());
            if (count > 0) {
                count--;
                recentEventTickCount.put(villager.getUUID(), count);
            } else {
                recentEventTickCount.remove(villager.getUUID());
                villager.getPersistentData().remove(SpeechManager.RECENT_EVENT_TAG);
            }
        }
    }


    private static List<Villager> findNearbyVillagers(Villager source, double range, double nearbyRange) {
        List<Villager> villagers = new ArrayList<>(source.level().getEntitiesOfClass(
                Villager.class,
                source.getBoundingBox().inflate(range)
        ).stream().toList());

        List<Villager> finalVillagers = new ArrayList<>();
        List<Villager> associatedVillagers = new ArrayList<>();
        for (Villager villager : villagers) {

            associatedVillagers = villager.level().getEntitiesOfClass(
                    Villager.class,
                    villager.getBoundingBox().inflate(nearbyRange)
            );
        }
        villagers.forEach(villager -> {
            if (!finalVillagers.contains(villager))
                finalVillagers.add(villager);
        });
        associatedVillagers.forEach(villager -> {
            if (!finalVillagers.contains(villager))
                finalVillagers.add(villager);
        });

        return finalVillagers;
    }
    private static Villager findNearestVillager(Entity source, double range) {
        return source.level().getEntitiesOfClass(
                        Villager.class,
                        source.getBoundingBox().inflate(range)
                ).stream()
                .min(Comparator.comparingDouble(v -> v.distanceTo(source)))
                .orElse(null);
    }
}

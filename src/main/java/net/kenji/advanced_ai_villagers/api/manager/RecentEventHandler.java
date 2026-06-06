package net.kenji.advanced_ai_villagers.api.manager;

import net.kenji.advanced_ai_villagers.AiTalkingVillagers;
import net.kenji.advanced_ai_villagers.api.context.villager_info.RecentEventContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

public class RecentEventHandler {
    public static Map<UUID, Integer> recentEventTickCount = new HashMap<>();
    public static int MAX_RECENT_EVENT_TIME = 24000 * 7;


    public static void tick(Villager villager){
        assignRaidSurvivedRecentEvent(villager);
        manageRecentEventMemoryCount(villager);
    }

    public static void assignVillagerDeathRecentEvent(Villager villager){
        List<Villager> nearbyVillagers = findNearbyVillagers(villager, 80, 40);

        for (Villager nearby : nearbyVillagers) {
            if (nearby != villager)
                nearby.getPersistentData().putString(SpeechManager.RECENT_EVENT_TAG, RecentEventContext.VILLAGER_DEATH.getTagName());
        }
    }

    public static void assignRaidSurvivedRecentEvent(Villager entity) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) return;

        Raid raid = serverLevel.getRaidAt(entity.blockPosition());

        if (raid != null) {
            if (raid.isOver()) {
                Villager villager = findNearestVillager(entity, 40);
                List<Villager> nearbyVillagers = findNearbyVillagers(villager, 80, 40);
                if (!nearbyVillagers.contains(villager))
                    nearbyVillagers.add(villager);
                for (Villager nearby : nearbyVillagers) {
                    nearby.getPersistentData().putString(SpeechManager.RECENT_EVENT_TAG, RecentEventContext.RAID_SURVIVED.getTagName());
                }
            }
        }
    }


    public static void manageRecentEventMemoryCount(Villager villager) {
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

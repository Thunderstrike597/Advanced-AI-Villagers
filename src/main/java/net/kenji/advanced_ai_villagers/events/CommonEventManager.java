package net.kenji.advanced_ai_villagers.events;

import net.kenji.advanced_ai_villagers.AiTalkingVillagers;
import net.kenji.advanced_ai_villagers.api.handler.PersonalityHandler;
import net.kenji.advanced_ai_villagers.api.manager.RecentEventHandler;
import net.kenji.advanced_ai_villagers.api.manager.SpeechManager;
import net.kenji.advanced_ai_villagers.api.handler.VillagerHurtHandler;
import net.kenji.advanced_ai_villagers.api.handler.VillagerSpeechHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.raid.Raider;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AiTalkingVillagers.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommonEventManager {
    @SubscribeEvent
    public static void onPlayerChat(ServerChatEvent event) {
        String message = event.getMessage().getString();
        ServerPlayer player =  event.getPlayer();
        SpeechManager.sendSpeechMessage(player, message, true);
    }

    @SubscribeEvent
    public static void onVillagerTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Villager villager)) return;

        VillagerSpeechHandler.tick(villager);
        RecentEventHandler.tick(villager);
    }

    @SubscribeEvent
    public static void onVillagerAttacked(LivingAttackEvent event) {
        VillagerHurtHandler.managerVillagerAttacked(event);
    }
    @SubscribeEvent
    public static void onLevelJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof Villager villager))
            return;
        PersonalityHandler.maybeAssignPersonality(villager);
    }
    @SubscribeEvent
    public static void onLivingDeathDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        if (event.getEntity() instanceof Villager villager) {
            RecentEventHandler.assignVillagerDeathRecentEvent(villager);
        }
    }
}

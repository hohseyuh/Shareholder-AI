package com.hohseyuh.mcai;

import com.hohseyuh.mcai.ai.NpcJob;
import com.hohseyuh.mcai.entity.ModEntities;
import com.hohseyuh.mcai.entity.custom.SimpleNpcEntity;
import com.hohseyuh.mcai.network.VoiceManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Box;

import java.util.List;

public class Mcai implements ModInitializer {
    public static final String MOD_ID = "mcai";
    private static VoiceManager voiceManager;

    @Override
    public void onInitialize() {
        ModEntities.registerModEntities();
        FabricDefaultAttributeRegistry.register(ModEntities.SIMPLE_NPC, SimpleNpcEntity.createNpcAttributes());

        // 1. Initialize Voice Manager when Server Starts
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            // Adjust the URL to match your Python API
            voiceManager = new VoiceManager(server);
            voiceManager.start();
            System.out.println("Mcai Voice Manager Started!");
        });

        // 2. Cleanup when Server Stops
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            if (voiceManager != null)
                voiceManager.stop();
        });

        // 3. Chat Listener (Passes text to the shared handler)
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            if (sender == null)
                return;
            // Execute on Main Thread
            sender.getServer().execute(() -> {
                handleCommand(sender, message.getContent().getString());
            });
        });
    }

    /**
     * CENTRAL COMMAND PROCESSOR
     * Both Chat and Voice call this method.
     */
    public static void handleCommand(ServerPlayerEntity sender, String rawCommand) {
        if (sender == null || rawCommand == null || rawCommand.isEmpty())
            return;

        Box box = sender.getBoundingBox().expand(15.0); // 15 Block Radius
        List<SimpleNpcEntity> npcs = sender.getWorld().getEntitiesByClass(SimpleNpcEntity.class, box, e -> true);

        if (npcs.isEmpty())
            return;

        String lower = rawCommand.toLowerCase().trim();
        System.out.println("Processing Command: " + lower); // Debug log

        // --- MOVEMENT ---
        if (lower.contains("follow")) {
            npcs.forEach(npc -> {
                npc.setFollowTarget(sender);
                npc.sendMessage("Following you!");
            });
            return;
        }
        if (lower.contains("stop") || lower.contains("stay") || lower.contains("wait")) {
            npcs.forEach(npc -> {
                npc.setFollowTarget(null);
                npc.getNavigation().stop();
                npc.sendMessage("Stopping.");
            });
            return;
        }

        // --- JOBS ---
        if (lower.contains("miner") || lower.contains("mine")) {
            npcs.forEach(npc -> npc.setJob(NpcJob.MINER));
            return;
        }
        if (lower.contains("guard") || lower.contains("protect")) {
            npcs.forEach(npc -> npc.setJob(NpcJob.GUARD));
            return;
        }
        if (lower.contains("relax") || lower.contains("no job")) {
            npcs.forEach(npc -> npc.setJob(NpcJob.NONE));
            return;
        }

        // --- ACTIONS ---
        if (lower.contains("drop") || lower.contains("loot") || lower.contains("stash")) {
            npcs.forEach(npc -> npc.commandDropLoot(sender));
            return;
        }
        if (lower.contains("unequip") || lower.contains("give tool")) {
            npcs.forEach(npc -> npc.commandUnequip(sender));
            return;
        }

        // --- RANKS ---
        if (lower.contains("promote")) {
            npcs.forEach(SimpleNpcEntity::promote);
            return;
        }
        if (lower.contains("demote")) {
            npcs.forEach(SimpleNpcEntity::demote);
            return;
        }
    }
}
package com.hohseyuh.mcai;

import com.hohseyuh.mcai.entity.ModEntities;
import com.hohseyuh.mcai.entity.custom.SimpleNpcEntity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class Mcai implements ModInitializer {
    public static final String MOD_ID = "mcai";

    @Override
    public void onInitialize() {
        // 1. Register the Entity Type
        ModEntities.registerModEntities();

        // 2. Register the Entity Attributes
        FabricDefaultAttributeRegistry.register(ModEntities.SIMPLE_NPC, SimpleNpcEntity.createNpcAttributes());

        // Chat Event Listener
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            if (sender == null)
                return;

            String content = message.getContent().getString();
            Box box = sender.getBoundingBox().expand(10.0);

            // Get all NPCs near the player
            List<SimpleNpcEntity> npcs = sender.getWorld().getEntitiesByClass(SimpleNpcEntity.class, box,
                    entity -> true);

            // If no NPCs nearby, ignore
            if (npcs.isEmpty())
                return;

            String lowerContent = content.toLowerCase().trim();

            // --- MINING COMMANDS ---
            if (lowerContent.contains("stop mining")) {
                for (SimpleNpcEntity npc : npcs) {
                    npc.stopMining();
                    String npcName = getNpcName(npc);
                    broadcast(sender, "<" + npcName + "> Okay, stopping mining.");
                }
                return;
            } else if (lowerContent.contains("start mining")) {
                for (SimpleNpcEntity npc : npcs) {
                    npc.startMining();
                    String npcName = getNpcName(npc);
                    broadcast(sender, "<" + npcName + "> Searching for ores...");
                }
                return;
            }

            // --- FOLLOW COMMANDS ---
            if (lowerContent.contains("follow me")) {
                for (SimpleNpcEntity npc : npcs) {
                    npc.setFollowTarget(sender);
                    String npcName = getNpcName(npc);
                    broadcast(sender, "<" + npcName + "> Coming!");
                }
                return;
            } else if (lowerContent.contains("stop following") || lowerContent.contains("stay here")) {
                for (SimpleNpcEntity npc : npcs) {
                    npc.setFollowTarget(null);
                    npc.stopMining();
                    npc.getNavigation().stop(); // Force stop immediately

                    String npcName = getNpcName(npc);
                    broadcast(sender, "<" + npcName + "> I'll stay here.");
                }
                return;
            }

            // --- CHAT / TALKING ---
            // Make NPCs look at player and stop moving while "processing" the message
            for (SimpleNpcEntity npc : npcs) {
                npc.getNavigation().stop();
                npc.getLookControl().lookAt(sender);
            }

            // Delayed Echo (Simulating thinking time)
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    // Must run back on the Main Server Thread
                    if (sender.getServer() != null) {
                        sender.getServer().execute(() -> {
                            for (SimpleNpcEntity npc : npcs) {
                                // Double check NPC is still alive
                                if (npc.isAlive()) {
                                    String npcName = getNpcName(npc);
                                    broadcast(sender, "<" + npcName + "> " + content);
                                }
                            }
                        });
                    }
                }
            }, 2000); // 2 seconds delay
        });
    }

    // Helper to avoid repeating name logic
    private String getNpcName(SimpleNpcEntity npc) {
        return npc.getCustomName() != null ? npc.getCustomName().getString() : "NPC";
    }

    // Helper to send message to everyone
    private void broadcast(net.minecraft.server.network.ServerPlayerEntity sender, String message) {
        sender.getServer().getPlayerManager().broadcast(Text.literal(message), false);
    }
}
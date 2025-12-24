package com.hohseyuh.mcai;

import com.hohseyuh.mcai.ai.NpcJob;
import com.hohseyuh.mcai.entity.ModEntities;
import com.hohseyuh.mcai.entity.custom.SimpleNpcEntity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.util.math.Box;

import java.util.List;

public class Mcai implements ModInitializer {
    public static final String MOD_ID = "mcai";

    @Override
    public void onInitialize() {
        ModEntities.registerModEntities();
        FabricDefaultAttributeRegistry.register(ModEntities.SIMPLE_NPC, SimpleNpcEntity.createNpcAttributes());

        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            if (sender == null)
                return;

            sender.getServer().execute(() -> {
                String content = message.getContent().getString();
                Box box = sender.getBoundingBox().expand(10.0);
                List<SimpleNpcEntity> npcs = sender.getWorld().getEntitiesByClass(SimpleNpcEntity.class, box,
                        e -> true);
                if (npcs.isEmpty())
                    return;

                String lower = content.toLowerCase().trim();

                // COMMANDS
                if (lower.contains("follow")) {
                    npcs.forEach(npc -> {
                        npc.setFollowTarget(sender);
                        npc.sendMessage("GELIOM!");
                    });
                    return;
                }
                if (lower.contains("stop") || lower.contains("stay")) {
                    npcs.forEach(npc -> {
                        npc.setFollowTarget(null);
                        npc.getNavigation().stop();
                        npc.sendMessage("tamam.");
                    });
                    return;
                }

                // Job Commands
                if (lower.contains("job miner") || lower.contains("work mine")) {
                    npcs.forEach(npc -> npc.setJob(NpcJob.MINER));
                    return;
                }
                if (lower.contains("job none") || lower.contains("stop work")) {
                    npcs.forEach(npc -> npc.setJob(NpcJob.NONE));
                    return;
                }

                // Action Commands
                if (lower.contains("unequip") || lower.contains("give pickaxe")) {
                    npcs.forEach(npc -> npc.commandUnequip(sender));
                    return;
                }
                if (lower.contains("give loot") || lower.contains("stash")) {
                    npcs.forEach(npc -> npc.commandDropLoot(sender));
                    return;
                }

                // Echo
                for (SimpleNpcEntity npc : npcs) {
                    npc.getNavigation().stop();
                    npc.getLookControl().lookAt(sender);
                    npc.setDelayedMessage("baba arama beni bu konulardan dolayi ya", 40);
                }
            });
        });
    }
}
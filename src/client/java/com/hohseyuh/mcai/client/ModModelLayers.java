package com.hohseyuh.mcai.client;

import com.hohseyuh.mcai.Mcai;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;

public class ModModelLayers {
    public static final EntityModelLayer SIMPLE_NPC = new EntityModelLayer(
            Identifier.of(Mcai.MOD_ID, "simple_npc"), "main");
}

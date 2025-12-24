package com.hohseyuh.mcai.entity;

import com.hohseyuh.mcai.Mcai; // Assuming this is your main class with MOD_ID
import com.hohseyuh.mcai.entity.custom.SimpleNpcEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModEntities {

    @SuppressWarnings("deprecation")
    public static final EntityType<SimpleNpcEntity> SIMPLE_NPC = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(Mcai.MOD_ID, "simple_npc"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, SimpleNpcEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6f, 1.8f)) // Hitbox size (Width, Height)
                    .build());

    public static void registerModEntities() {
        // This method is called in the Main class to initialize the registration
        // You can add a logger here if you want to debug startup
        System.out.println("Registering Entities for " + "mcai");
    }
}
package com.hohseyuh.mcai.client;

import com.hohseyuh.mcai.client.model.SimpleNpcModel;
import com.hohseyuh.mcai.client.renderer.SimpleNpcRenderer;
import com.hohseyuh.mcai.entity.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class McaiClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.SIMPLE_NPC, SimpleNpcModel::getTexturedModelData);
        EntityRendererRegistry.register(ModEntities.SIMPLE_NPC, SimpleNpcRenderer::new);
    }
}

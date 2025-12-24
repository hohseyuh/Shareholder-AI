package com.hohseyuh.mcai.client.renderer;

import com.hohseyuh.mcai.client.ModModelLayers;
import com.hohseyuh.mcai.client.model.SimpleNpcModel;
import com.hohseyuh.mcai.entity.custom.SimpleNpcEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.util.Identifier;

// 1. Ensure SimpleNpcModel extends BipedEntityModel<SimpleNpcEntity> in its own class file!
public class SimpleNpcRenderer extends MobEntityRenderer<SimpleNpcEntity, SimpleNpcModel> {

    // Note: 'Identifier.of' is correct for 1.21+, use 'new Identifier' for 1.20.4
    // and older
    private static final Identifier TEXTURE = Identifier.of("mcai", "textures/entity/simple_npc/simple_npc.png");

    public SimpleNpcRenderer(EntityRendererFactory.Context context) {
        super(context, new SimpleNpcModel(context.getPart(ModModelLayers.SIMPLE_NPC)), 0.5f);

        // FIX: Add <SimpleNpcEntity> to BipedEntityModel calls
        // This tells the compiler: "These armor models are specifically for
        // SimpleNpcEntity"
        this.addFeature(new ArmorFeatureRenderer<>(this,
                new BipedEntityModel<SimpleNpcEntity>(context.getPart(EntityModelLayers.PLAYER_INNER_ARMOR)),
                new BipedEntityModel<SimpleNpcEntity>(context.getPart(EntityModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager() // Check: .getModelLoader() or .getModelManager() depending on version
        ));

        this.addFeature(new HeldItemFeatureRenderer<>(this, context.getHeldItemRenderer()));
    }

    @Override
    public Identifier getTexture(SimpleNpcEntity entity) {
        return TEXTURE;
    }
}
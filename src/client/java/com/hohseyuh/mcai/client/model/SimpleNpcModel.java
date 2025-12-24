package com.hohseyuh.mcai.client.model;

import com.hohseyuh.mcai.entity.custom.SimpleNpcEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.BipedEntityModel;

public class SimpleNpcModel extends BipedEntityModel<SimpleNpcEntity> {
    public SimpleNpcModel(ModelPart root) {
        super(root);
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = BipedEntityModel.getModelData(Dilation.NONE, 0f);
        return TexturedModelData.of(modelData, 64, 64);
    }
}

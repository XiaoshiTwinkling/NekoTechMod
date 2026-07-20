package com.nekotech.renderer;

import com.nekotech.entity.CatCameraBodyEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class CatCameraBodyEntityRenderer extends LivingEntityRenderer<CatCameraBodyEntity, PlayerEntityModel<CatCameraBodyEntity>> {
    private final PlayerEntityModel<CatCameraBodyEntity> wideModel;
    private final PlayerEntityModel<CatCameraBodyEntity> slimModel;

    public CatCameraBodyEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new PlayerEntityModel<>(context.getPart(EntityModelLayers.PLAYER), false), 0.5F);
        this.wideModel = this.model;
        this.slimModel = new PlayerEntityModel<>(context.getPart(EntityModelLayers.PLAYER_SLIM), true);
        addFeature(new HeldItemFeatureRenderer<>(this, context.getHeldItemRenderer()));
        addFeature(new ArmorFeatureRenderer<>(this,
                new BipedEntityModel<>(context.getPart(EntityModelLayers.PLAYER_INNER_ARMOR)),
                new BipedEntityModel<>(context.getPart(EntityModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()));
    }

    @Override
    public void render(CatCameraBodyEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        this.model = getSkinTextures(entity).model() == SkinTextures.Model.SLIM ? slimModel : wideModel;
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    @Override
    public Identifier getTexture(CatCameraBodyEntity entity) {
        return getSkinTextures(entity).texture();
    }

    private SkinTextures getSkinTextures(CatCameraBodyEntity entity) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() != null) {
            var entry = client.getNetworkHandler().getPlayerListEntry(entity.getOwnerUuid());
            if (entry != null) return entry.getSkinTextures();
        }
        return DefaultSkinHelper.getSkinTextures(entity.getOwnerUuid());
    }
}

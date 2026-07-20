package com.nekotech.renderer;

import com.nekotech.catcamera.CatCameraChannelAccess;
import com.nekotech.catcamera.CatCameraClientState;
import com.nekotech.mixin.client.OcelotEntityModelAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.CatEntityModel;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.RotationAxis;

public class CatCameraFeatureRenderer extends FeatureRenderer<CatEntity, CatEntityModel<CatEntity>> {
    private static final ItemStack RENDER_CONTEXT_STACK = new ItemStack(Items.STONE);

    public CatCameraFeatureRenderer(FeatureRendererContext<CatEntity, CatEntityModel<CatEntity>> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CatEntity cat,
                       float limbAngle, float limbDistance, float tickDelta, float age,
                       float headYaw, float headPitch) {
        if (!(cat instanceof CatCameraChannelAccess access) || !access.neko_technology$isCatCameraChannelActive()) return;
        if (CatCameraClientState.isActive() && cat.getUuid().equals(CatCameraClientState.getCatUuid())) return;

        matrices.push();
        ((OcelotEntityModelAccessor) getContextModel()).neko_technology$getHead().rotate(matrices);
        matrices.translate(0.0D, -0.30D, -0.10D);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180.0F));
        matrices.scale(0.30F, 0.30F, 0.30F);

        var itemRenderer = MinecraftClient.getInstance().getItemRenderer();
        BakedModel model = CatCameraModels.getCameraModel();
        itemRenderer.renderItem(RENDER_CONTEXT_STACK, ModelTransformationMode.FIXED, false, matrices,
                vertexConsumers, light, OverlayTexture.DEFAULT_UV, model);
        matrices.pop();
    }
}

package com.nekotech.renderer.blockentities;

import com.nekotech.block.entity.machines.BellowsBlockEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.random.Random;

@Environment(EnvType.CLIENT)
public class BellowsBlockEntityRenderer implements BlockEntityRenderer<BellowsBlockEntity> {

    private final BlockRenderManager brm;
    public BellowsBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
        this.brm = ctx.getRenderManager();
    }

    private static final Random RANDOM = Random.create();

    @Override
    public void render(BellowsBlockEntity entity, float tickDelta,
                       MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                       int light, int overlay) {

        BlockState state = entity.getCachedState();
        BakedModel model = brm.getModel(state);

        float progress = entity.getRenderProgress(tickDelta);

        float eased = (float)Math.pow(progress, 2.5f);

        float compression = 0.6f; // 压缩强度
        float bulge = 0.18f;      // 横向膨胀强度

        float scaleY = 1.0f - compression * eased;

        float scaleXZ = 1.0f + bulge * eased;

        matrices.push();

        matrices.translate(0.5f, 0.0f, 0.5f);
        matrices.scale(scaleXZ, scaleY, scaleXZ);
        matrices.translate(-0.5f, 0.0f, -0.5f);

        brm.getModelRenderer().render(
                entity.getWorld(),
                model,
                state,
                entity.getPos(),
                matrices,
                vertexConsumers.getBuffer(RenderLayers.getBlockLayer(state)),
                false,
                RANDOM,
                state.getRenderingSeed(entity.getPos()),
                OverlayTexture.DEFAULT_UV
        );

        matrices.pop();
    }
}

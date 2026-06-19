package com.nekotech.renderer.blockentities;

import com.nekotech.block.entity.machines.BellowsBlockEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;
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
        Direction facing = state.get(Properties.FACING);
        BakedModel model = brm.getModel(state);

        float progress = entity.getRenderProgress(tickDelta);
        float eased = (float) Math.pow(progress, 2.5f);
        float compression = 0.6f;
        float bulge = 0.18f;
        float scaleCompress = 1.0f - compression * eased;
        float scaleExpand = 1.0f + bulge * eased;

        matrices.push();

        matrices.translate(0.5, 0.5, 0.5);

        switch (facing) {
            case DOWN -> {
                matrices.translate(0, -0.5, 0);
                matrices.scale(scaleExpand, scaleCompress, scaleExpand);
                matrices.translate(0, 0.5, 0);
            }
            case UP -> {
                matrices.translate(0, 0.5, 0);
                matrices.scale(scaleExpand, scaleCompress, scaleExpand);
                matrices.translate(0, -0.5, 0);
            }
            case NORTH -> {
                matrices.translate(0, 0, -0.5);
                matrices.scale(scaleExpand, scaleExpand, scaleCompress);
                matrices.translate(0, 0, 0.5);
            }
            case SOUTH -> {
                matrices.translate(0, 0, 0.5);
                matrices.scale(scaleExpand, scaleExpand, scaleCompress);
                matrices.translate(0, 0, -0.5);
            }
            case WEST -> {
                matrices.translate(-0.5, 0, 0);
                matrices.scale(scaleCompress, scaleExpand, scaleExpand);
                matrices.translate(0.5, 0, 0);
            }
            case EAST -> {
                matrices.translate(0.5, 0, 0);
                matrices.scale(scaleCompress, scaleExpand, scaleExpand);
                matrices.translate(-0.5, 0, 0);
            }
        }

        matrices.translate(-0.5, -0.5, -0.5);

        brm.getModelRenderer().render(
                entity.getWorld(),
                model,
                state,
                entity.getPos(),
                matrices,
                vertexConsumers.getBuffer(RenderLayer.getSolid()),
                false,
                RANDOM,
                state.getRenderingSeed(entity.getPos()),
                OverlayTexture.DEFAULT_UV
        );

        matrices.pop();
    }
}

package com.nekotech.renderer.blockentities;

import com.nekotech.block.entity.ElevatorCoreBlockEntity;
import com.nekotech.block.custom.ModBlocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;

public class ElevatorCoreBlockEntityRenderer implements BlockEntityRenderer<ElevatorCoreBlockEntity> {
    public ElevatorCoreBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
    }

    @Override
    public void render(
            ElevatorCoreBlockEntity entity,
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            int overlay
    ) {
        double floor = entity.getCabinFloor(tickDelta);

        matrices.push();

        /*
         * 动态轿厢相对于核心方块移动。
         */
        matrices.translate(0.10D, floor + 0.10D, 0.10D);
        matrices.scale(0.80F, 0.80F, 0.80F);

        MinecraftClient.getInstance()
                .getBlockRenderManager()
                .renderBlockAsEntity(
                        ModBlocks.ELEVATOR_CORE_BOX.getDefaultState(),
                        matrices,
                        vertexConsumers,
                        light,
                        OverlayTexture.DEFAULT_UV
                );

        matrices.pop();
    }

    @Override
    public boolean rendersOutsideBoundingBox(ElevatorCoreBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getRenderDistance() {
        return 64;
    }
}
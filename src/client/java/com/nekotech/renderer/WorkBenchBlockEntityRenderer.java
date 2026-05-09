package com.nekotech.renderer;

import com.nekotech.block.entity.machines.WorkBenchBlockEntity;
import com.nekotech.item.block.WorkBench;
import net.minecraft.block.entity.VaultBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;

public class WorkBenchBlockEntityRenderer implements BlockEntityRenderer<WorkBenchBlockEntity> {
    public WorkBenchBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
    }

    @Override
    public void render(WorkBenchBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {

        Direction facing = entity.getCachedState().get(WorkBench.FACING);
        float rotationAngle = -facing.asRotation(); // 转换为渲染旋转角度

        ItemStack inputStack = entity.getStack(WorkBenchBlockEntity.INPUT_SLOT);
        if (!inputStack.isEmpty()) {
            renderItemOnBench(entity, inputStack, rotationAngle, matrices, vertexConsumers, light,
                    -0.25f, 0.0f, -0.05f);
        }

        ItemStack outputStack = entity.getStack(WorkBenchBlockEntity.OUTPUT_SLOT);
        if (!outputStack.isEmpty()) {
            renderItemOnBench(entity, outputStack, rotationAngle, matrices, vertexConsumers, light,
                    0.25f, 0.0f, 0.05f);
        }
    }

    private void renderItemOnBench(WorkBenchBlockEntity entity,ItemStack stack, float yaw, MatrixStack matrices,
                                   VertexConsumerProvider vertexConsumers, int light,
                                   float xOffset, float yOffset, float zOffset) {

        matrices.push();

        matrices.translate(0.5, 0.8, 0.5);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw));
        matrices.translate(xOffset, yOffset, zOffset);
        matrices.scale(0.5f, 0.5f, 0.5f);
        MinecraftClient.getInstance().getItemRenderer().renderItem(
                stack,
                ModelTransformationMode.FIXED,
                light,
                OverlayTexture.DEFAULT_UV,
                matrices,
                vertexConsumers,
                entity.getWorld(),
                0
        );

        matrices.pop();
    }
}

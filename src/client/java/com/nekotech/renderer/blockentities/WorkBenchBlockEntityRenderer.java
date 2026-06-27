package com.nekotech.renderer.blockentities;

import com.nekotech.block.entity.machines.WorkBenchBlockEntity;
import com.nekotech.block.custom.WorkBench;
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
        float rotationAngle = -facing.asRotation();

        ItemStack inputStack = entity.getStack(WorkBenchBlockEntity.INPUT_SLOT);
            renderItemOnBench(entity, inputStack, rotationAngle, matrices, vertexConsumers, light,
                    0f, 0.0f, 0f);

        ItemStack outputStack = entity.getStack(WorkBenchBlockEntity.OUTPUT_SLOT);
            renderItemOnBench(entity, outputStack, rotationAngle, matrices, vertexConsumers, light,
                    0.31f, 0.0f, -0.23f);
    }

    private void renderItemOnBench(WorkBenchBlockEntity entity,ItemStack stack, float yaw, MatrixStack matrices,
                                   VertexConsumerProvider vertexConsumers, int light,
                                   float xOffset, float yOffset, float zOffset) {

        if(stack.isEmpty()){
            return;
        }

        matrices.push();
        matrices.translate(0.5, 0.8, 0.5);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw));
        matrices.translate(xOffset, yOffset, zOffset);
        matrices.scale(0.5f, 0.5f, 0.5f);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90f));

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

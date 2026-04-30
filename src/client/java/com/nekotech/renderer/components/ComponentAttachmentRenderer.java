package com.nekotech.renderer.components;

import com.nekotech.block.entity.machines.FluxStorageBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;

public class ComponentAttachmentRenderer implements BlockEntityRenderer<FluxStorageBlockEntity> {

    @Override
    public void render(FluxStorageBlockEntity blockEntity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        if (blockEntity.getAttachedComponents().isEmpty()) {
            return;
        }

        // 遍历六个面
        for (Direction side : Direction.values()) {
            var componentItem = blockEntity.getComponent(side);
            if (componentItem != null) {
                renderComponent(matrices, vertexConsumers, componentItem, side, light, overlay);
            }
        }
    }

    private void renderComponent(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                 net.minecraft.item.Item componentItem, Direction side,
                                 int light, int overlay) {
        matrices.push();

        // 移动到方块中心
        matrices.translate(0.5f, 0.5f, 0.5f);

        // 根据面应用旋转
        switch (side) {
            case DOWN -> matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
            case UP -> matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90));
            case NORTH -> matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180));
            case SOUTH -> {}
            case WEST -> matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-90));
            case EAST -> matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90));
        }

        // 从方块表面向外偏移
        matrices.translate(0, 0, 0.3f);

        matrices.scale(1f, 1f, 1f);

        // 渲染物品
        ItemStack stack = new ItemStack(componentItem, 1);
        MinecraftClient.getInstance().getItemRenderer().renderItem(
                stack,
                ModelTransformationMode.FIXED,
                light,
                overlay,
                matrices,
                vertexConsumers,
                MinecraftClient.getInstance().world,
                0
        );

        matrices.pop();
    }
}

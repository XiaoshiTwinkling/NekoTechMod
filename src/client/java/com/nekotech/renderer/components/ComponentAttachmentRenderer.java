package com.nekotech.renderer.components;

import com.nekotech.block.entity.api.component.ComponentAdaptation;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.world.LightType;

public class ComponentAttachmentRenderer implements BlockEntityRenderer<BlockEntity> {

    @Override
    public void render(BlockEntity blockEntity, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {

        // 检查方块实体是否实现了ComponentAdaptation接口
        if (!(blockEntity instanceof ComponentAdaptation componentAdaptation)) {
            return;  // 不是零件宿主，跳过渲染
        }

        if (componentAdaptation.getAttachedComponents().isEmpty()) {
            return;  // 没有安装零件，跳过渲染
        }

        // 遍历六个面
        for (Direction side : Direction.values()) {
            var componentItem = componentAdaptation.getComponent(side);
            if (componentItem != null) {
                int faceLight = calculateFaceLight(blockEntity, side);
                renderComponent(matrices, vertexConsumers, componentItem, side, faceLight, overlay);
            }
        }
    }

    /**
     * 计算指定面的正确光照值
     */
    private int calculateFaceLight(BlockEntity blockEntity, Direction side) {
        if (blockEntity.getWorld() == null) {
            return LightmapTextureManager.MAX_LIGHT_COORDINATE; // 默认最大光照
        }

        BlockPos pos = blockEntity.getPos();
        BlockPos neighborPos = pos.offset(side);

        // 获取方块光（Block Light）和天空光（Skylight）
        int blockLight = blockEntity.getWorld().getLightLevel(LightType.BLOCK, neighborPos);
        int skyLight = blockEntity.getWorld().getLightLevel(LightType.SKY, neighborPos);

        // 将两个光照值组合成Minecraft使用的光照坐标
        return LightmapTextureManager.pack(blockLight, skyLight);
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
        matrices.translate(0, 0, 0.1f);

        matrices.scale(0.9f, 0.9f, 0.9f);

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

package com.nekotech.renderer.blockentities;

import com.nekotech.NekoTechnology;
import com.nekotech.NekoTechnologyClient;
import com.nekotech.block.custom.CatGeneratorBlock;
import com.nekotech.block.custom.CatGeneratorPart;
import com.nekotech.block.entity.api.component.ComponentAdaptation;
import com.nekotech.block.entity.machines.CatGeneratorBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.world.LightType;

public class CatGeneratorTrackRenderer implements BlockEntityRenderer<CatGeneratorBlockEntity> {
    // 履带纹理路径
    private static final Identifier TRACK_TEXTURE = Identifier.of("neko-technology", "textures/block/cat_generator/generator_track.png");

    // 履带滚动速度
    private static final float TRACK_SPEED = 0.2f;

    private static final double TRACK_Y = 2 / 16.0;

    private static final double TRACK_THICKNESS = 0.5 / 16.0;

    private static final double TRACK_WIDTH = 14.0 / 16.0;
    private static final double TRACK_LENGTH = 14.0 / 16.0;

    public CatGeneratorTrackRenderer(BlockEntityRendererFactory.Context ctx) {}

    @Override
    public void render(CatGeneratorBlockEntity entity, float tickDelta,
                       MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                       int light, int overlay) {

        Direction facing = entity.getCachedState().get(CatGeneratorBlock.FACING);
        CatGeneratorPart part = entity.getCachedState().get(CatGeneratorBlock.PART);

        double pixelOffset = (part == CatGeneratorPart.LEFT ? -1.0 : 1.0) / 16.0;

        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(TRACK_TEXTURE));

        matrices.push();

        matrices.translate(0.5, TRACK_Y, 0.5);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-facing.asRotation()));
        matrices.translate(pixelOffset, 0, 0);
        matrices.translate(-0.5, 0, -0.5);

        long time = entity.getWorld() != null ? entity.getWorld().getTime() : 0;
        float uOffset = 0f;

        if (entity.isCatRunning()) {
            NekoTechnology.LOGGER.info("111");
            float catSpeed = entity.getCatRunningSpeed();
            float effectiveSpeed = TRACK_SPEED * Math.max(catSpeed, 0.1f);
            uOffset = -((time + tickDelta) * effectiveSpeed % 1.0f);
        }

        double minX = (1.0 - TRACK_WIDTH) / 2.0;
        double maxX = minX + TRACK_WIDTH;
        double minZ = (1.0 - TRACK_LENGTH) / 2.0;
        double maxZ = minZ + TRACK_LENGTH;
        double y = TRACK_Y;
        double yTop = y + TRACK_THICKNESS;

        addFace(vertexConsumer, matrices,
                (float) minX, (float) yTop, (float) minZ,
                (float) maxX, (float) yTop, (float) minZ,
                (float) maxX, (float) yTop, (float) maxZ,
                (float) minX, (float) yTop, (float) maxZ,
                1.0f, 1.0f, 1.0f, 1.0f,
                0.0f + uOffset, 0.0f,
                1.0f + uOffset, 0.0f,
                1.0f + uOffset, 1.0f,
                0.0f + uOffset, 1.0f,
                light, overlay);

        matrices.pop();

        if (((ComponentAdaptation) entity).getAttachedComponents().isEmpty()) {
            return;
        }

        for (Direction side : Direction.values()) {
            var componentItem = ((ComponentAdaptation) entity).getComponent(side);
            if (componentItem != null) {
                int faceLight = calculateFaceLight(entity, side);
                renderComponent(matrices, vertexConsumers, componentItem, side, faceLight, overlay);
            }
        }
    }

    /**
     * 添加一个四边形面
     */
    private void addFace(VertexConsumer consumer, MatrixStack matrices,
                         float x1, float y1, float z1,
                         float x2, float y2, float z2,
                         float x3, float y3, float z3,
                         float x4, float y4, float z4,
                         float r, float g, float b, float a,
                         float u1, float v1,
                         float u2, float v2,
                         float u3, float v3,
                         float u4, float v4,
                         int light, int overlay) {

        var entry = matrices.peek();
        var mat   = entry.getPositionMatrix();

        consumer.vertex(mat, x1, y1, z1)
                .color(r, g, b, a).texture(u1, v1)
                .overlay(overlay)
                .light(light)
                .normal(0f, 1f, 0f);

        consumer.vertex(mat, x2, y2, z2)
                .color(r, g, b, a).texture(u2, v2)
                .overlay(overlay)
                .light(light)
                .normal(0f, 1f, 0f);

        consumer.vertex(mat, x3, y3, z3)
                .color(r, g, b, a).texture(u3, v3)
                .overlay(overlay)
                .light(light)
                .normal(0f, 1f, 0f);

        consumer.vertex(mat, x4, y4, z4)
                .color(r, g, b, a).texture(u4, v4)
                .overlay(overlay)
                .light(light)
                .normal(0f, 1f, 0f);
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

        int blockLight = blockEntity.getWorld().getLightLevel(LightType.BLOCK, neighborPos);
        int skyLight = blockEntity.getWorld().getLightLevel(LightType.SKY, neighborPos);

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

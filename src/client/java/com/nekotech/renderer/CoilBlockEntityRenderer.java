package com.nekotech.renderer;

import com.nekotech.NekoTechnology;
import com.nekotech.block.entity.api.component.ComponentAdaptation;
import com.nekotech.block.entity.machines.coil.CoilBlockEntity;
import com.nekotech.block.entity.machines.coil.CoilType;
import com.nekotech.item.ModItems;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.world.LightType;
import org.joml.Matrix4f;

public class CoilBlockEntityRenderer implements BlockEntityRenderer<CoilBlockEntity> {
    private static final Identifier TEXTURE_COPPER = Identifier.of("neko-technology", "textures/block/coil/coil_copper_side.png");
    private static final Identifier TEXTURE_PIG_IRON = Identifier.of("neko-technology", "textures/block/coil/coil_pig_iron_side.png");
    private static final Identifier TEXTURE_NEKO_COPPER = Identifier.of("neko-technology", "textures/block/coil/coil_neko_copper_side.png");

    private static final float CORE_RADIUS = 2.0f / 16.0f;
    private static final float LAYER_THICKNESS = 1.0f / 16.0f;

    public CoilBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
    }

    @Override
    public void render(CoilBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        matrices.push();
        var coils = entity.getCoils();
        int totalFilledLayers = 0;
        for (CoilType coil : coils) {
            if (coil != CoilType.EMPTY) totalFilledLayers++;
        }

        for (int layerIndex = 0; layerIndex < coils.size(); layerIndex++) {
            CoilType coilType = coils.get(layerIndex);
            if (coilType == CoilType.EMPTY) break;

            float currentOuterRadius = CORE_RADIUS + (layerIndex + 1) * LAYER_THICKNESS;
            float currentInnerRadius = CORE_RADIUS + layerIndex * LAYER_THICKNESS;
            Identifier layerTexture = getTextureForType(coilType);

            boolean isOutermostLayer = (layerIndex == totalFilledLayers - 1);

            renderHollowCubeLayer(matrices, vertexConsumers, layerTexture,
                    currentInnerRadius, currentOuterRadius, light, overlay,
                    entity.getWorld().getTime() + tickDelta, entity.getPos(),
                    isOutermostLayer);
        }

        renderAttachedComponents(entity, matrices, vertexConsumers, light, overlay);

        matrices.pop();
    }

    private void renderHollowCubeLayer(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                       Identifier texture, float innerR, float outerR,
                                       int light, int overlay, float worldTime, BlockPos pos,
                                       boolean isOutermostLayer) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutout(texture));

        float xOuterNeg = 0.5f - outerR;
        float xOuterPos = 0.5f + outerR;
        float zOuterNeg = 0.5f - outerR;
        float zOuterPos = 0.5f + outerR;

        float xInnerNeg = 0.5f - innerR;
        float xInnerPos = 0.5f + innerR;
        float zInnerNeg = 0.5f - innerR;
        float zInnerPos = 0.5f + innerR;

        float yBottom = 0.0f;
        float yTop = 1.0f;

        buildTopRingFace(matrix, vertexConsumer, xInnerNeg, xInnerPos, zInnerNeg, zInnerPos,
                xOuterNeg, xOuterPos, zOuterNeg, zOuterPos, yTop, pos, light, overlay);
        buildBottomRingFace(matrix, vertexConsumer, xInnerNeg, xInnerPos, zInnerNeg, zInnerPos,
                xOuterNeg, xOuterPos, zOuterNeg, zOuterPos, yBottom, pos, light, overlay);

        if (isOutermostLayer) {
            buildWestFace(matrix, vertexConsumer, xOuterNeg, zOuterNeg, zOuterPos, yBottom, yTop, pos, light, overlay);
            buildEastFace(matrix, vertexConsumer, xOuterPos, zOuterNeg, zOuterPos, yBottom, yTop, pos, light, overlay);
            buildNorthFace(matrix, vertexConsumer, zOuterNeg, xOuterNeg, xOuterPos, yBottom, yTop, pos, light, overlay);
            buildSouthFace(matrix, vertexConsumer, zOuterPos, xOuterNeg, xOuterPos, yBottom, yTop, pos, light, overlay);
        }
    }

    private void buildWestFace(Matrix4f matrix, VertexConsumer consumer, float x, float zStart, float zEnd, float yStart, float yEnd, BlockPos pos, int light, int overlay) {
        float nx = -1.0f, ny = 0.0f, nz = 0.0f;
        addVertex(matrix, consumer, x, yStart, zStart, 0.0f, 0.0f, nx, ny, nz, light, overlay);
        addVertex(matrix, consumer, x, yStart, zEnd, 1.0f, 0.0f, nx, ny, nz, light, overlay);
        addVertex(matrix, consumer, x, yEnd, zEnd, 1.0f, 1.0f, nx, ny, nz, light, overlay);
        addVertex(matrix, consumer, x, yEnd, zStart, 0.0f, 1.0f, nx, ny, nz, light, overlay);
    }

    private void buildEastFace(Matrix4f matrix, VertexConsumer consumer, float x, float zStart, float zEnd, float yStart, float yEnd, BlockPos pos, int light, int overlay) {
        float nx = 1.0f, ny = 0.0f, nz = 0.0f;
        addVertex(matrix, consumer, x, yStart, zEnd, 0.0f, 0.0f, nx, ny, nz, light, overlay);
        addVertex(matrix, consumer, x, yStart, zStart, 1.0f, 0.0f, nx, ny, nz, light, overlay);
        addVertex(matrix, consumer, x, yEnd, zStart, 1.0f, 1.0f, nx, ny, nz, light, overlay);
        addVertex(matrix, consumer, x, yEnd, zEnd, 0.0f, 1.0f, nx, ny, nz, light, overlay);
    }

    private void buildNorthFace(Matrix4f matrix, VertexConsumer consumer, float z, float xStart, float xEnd, float yStart, float yEnd, BlockPos pos, int light, int overlay) {
        float nx = 0.0f, ny = 0.0f, nz = -1.0f;
        addVertex(matrix, consumer, xEnd, yStart, z, 0.0f, 0.0f, nx, ny, nz, light, overlay);
        addVertex(matrix, consumer, xStart, yStart, z, 1.0f, 0.0f, nx, ny, nz, light, overlay);
        addVertex(matrix, consumer, xStart, yEnd, z, 1.0f, 1.0f, nx, ny, nz, light, overlay);
        addVertex(matrix, consumer, xEnd, yEnd, z, 0.0f, 1.0f, nx, ny, nz, light, overlay);
    }

    private void buildSouthFace(Matrix4f matrix, VertexConsumer consumer, float z, float xStart, float xEnd, float yStart, float yEnd, BlockPos pos, int light, int overlay) {
        float nx = 0.0f, ny = 0.0f, nz = 1.0f;
        addVertex(matrix, consumer, xStart, yStart, z, 0.0f, 0.0f, nx, ny, nz, light, overlay);
        addVertex(matrix, consumer, xEnd, yStart, z, 1.0f, 0.0f, nx, ny, nz, light, overlay);
        addVertex(matrix, consumer, xEnd, yEnd, z, 1.0f, 1.0f, nx, ny, nz, light, overlay);
        addVertex(matrix, consumer, xStart, yEnd, z, 0.0f, 1.0f, nx, ny, nz, light, overlay);
    }

    private void buildTopRingFace(Matrix4f matrix, VertexConsumer consumer,
                                  float xInnerNeg, float xInnerPos, float zInnerNeg, float zInnerPos,
                                  float xOuterNeg, float xOuterPos, float zOuterNeg, float zOuterPos,
                                  float y, BlockPos pos, int light, int overlay) {
        float nx = 0.0f, ny = 1.0f, nz = 0.0f;
        float textureV = getVForWorldY(pos, y);

        addVertex(matrix, consumer, xOuterNeg, y, zOuterNeg, 0.0f, textureV, nx, ny, nz, light, overlay);
        addVertex(matrix, consumer, xOuterNeg, y, zInnerNeg, 0.0f, textureV, nx, ny, nz, light, overlay);
        addVertex(matrix, consumer, xOuterPos, y, zInnerNeg, 1.0f, textureV, nx, ny, nz, light, overlay);
        addVertex(matrix, consumer, xOuterPos, y, zOuterNeg, 1.0f, textureV, nx, ny, nz, light, overlay);

        addVertex(matrix, consumer, xOuterNeg, y, zInnerPos, 0.0f, textureV, nx, ny, nz, light, overlay);
        addVertex(matrix, consumer, xOuterNeg, y, zOuterPos, 0.0f, textureV, nx, ny, nz, light, overlay);
        addVertex(matrix, consumer, xOuterPos, y, zOuterPos, 1.0f, textureV, nx, ny, nz, light, overlay);
        addVertex(matrix, consumer, xOuterPos, y, zInnerPos, 1.0f, textureV, nx, ny, nz, light, overlay);

        addVertex(matrix, consumer, xOuterNeg, y, zInnerNeg, 0.0f, textureV, nx, ny, nz, light, overlay);
        addVertex(matrix, consumer, xOuterNeg, y, zInnerPos, 0.0f, textureV, nx, ny, nz, light, overlay);
        addVertex(matrix, consumer, xInnerNeg, y, zInnerPos, 1.0f, textureV, nx, ny, nz, light, overlay);
        addVertex(matrix, consumer, xInnerNeg, y, zInnerNeg, 1.0f, textureV, nx, ny, nz, light, overlay);

        addVertex(matrix, consumer, xInnerPos, y, zInnerNeg, 0.0f, textureV, nx, ny, nz, light, overlay);
        addVertex(matrix, consumer, xInnerPos, y, zInnerPos, 0.0f, textureV, nx, ny, nz, light, overlay);
        addVertex(matrix, consumer, xOuterPos, y, zInnerPos, 1.0f, textureV, nx, ny, nz, light, overlay);
        addVertex(matrix, consumer, xOuterPos, y, zInnerNeg, 1.0f, textureV, nx, ny, nz, light, overlay);
    }

    private void buildBottomRingFace(Matrix4f matrix, VertexConsumer consumer,
                                     float xInnerNeg, float xInnerPos, float zInnerNeg, float zInnerPos,
                                     float xOuterNeg, float xOuterPos, float zOuterNeg, float zOuterPos,
                                     float y, BlockPos pos, int light, int overlay) {
        float nx = 0.0f, ny = -1.0f, nz = 0.0f;
        float textureV = getVForWorldY(pos, y);

        addVertex(matrix, consumer, xOuterNeg, y, zOuterNeg, 0.0f, textureV, nx, ny, nz, light, overlay);
        addVertex(matrix, consumer, xOuterPos, y, zOuterNeg, 1.0f, textureV, nx, ny, nz, light, overlay);
        addVertex(matrix, consumer, xOuterPos, y, zInnerNeg, 1.0f, textureV, nx, ny, nz, light, overlay);
        addVertex(matrix, consumer, xOuterNeg, y, zInnerNeg, 0.0f, textureV, nx, ny, nz, light, overlay);

        addVertex(matrix, consumer, xOuterNeg, y, zInnerPos, 0.0f, textureV, nx, ny, nz, light, overlay);
        addVertex(matrix, consumer, xOuterPos, y, zInnerPos, 1.0f, textureV, nx, ny, nz, light, overlay);
        addVertex(matrix, consumer, xOuterPos, y, zOuterPos, 1.0f, textureV, nx, ny, nz, light, overlay);
        addVertex(matrix, consumer, xOuterNeg, y, zOuterPos, 0.0f, textureV, nx, ny, nz, light, overlay);

        addVertex(matrix, consumer, xOuterNeg, y, zInnerNeg, 0.0f, textureV, nx, ny, nz, light, overlay);
        addVertex(matrix, consumer, xInnerNeg, y, zInnerNeg, 1.0f, textureV, nx, ny, nz, light, overlay);
        addVertex(matrix, consumer, xInnerNeg, y, zInnerPos, 1.0f, textureV, nx, ny, nz, light, overlay);
        addVertex(matrix, consumer, xOuterNeg, y, zInnerPos, 0.0f, textureV, nx, ny, nz, light, overlay);

        addVertex(matrix, consumer, xInnerPos, y, zInnerNeg, 0.0f, textureV, nx, ny, nz, light, overlay);
        addVertex(matrix, consumer, xOuterPos, y, zInnerNeg, 1.0f, textureV, nx, ny, nz, light, overlay);
        addVertex(matrix, consumer, xOuterPos, y, zInnerPos, 1.0f, textureV, nx, ny, nz, light, overlay);
        addVertex(matrix, consumer, xInnerPos, y, zInnerPos, 0.0f, textureV, nx, ny, nz, light, overlay);
    }

    /**
     * @param u 纹理U坐标 (0~1)
     * @param v 纹理V坐标 (0~1)
     */
    private void addVertex(Matrix4f matrix, VertexConsumer consumer, float x, float y, float z, float u, float v, float nx, float ny, float nz, int light, int overlay) {
        consumer.vertex(matrix, x, y, z)
                .color(1.0f, 1.0f, 1.0f, 1.0f)
                .texture(u, v)
                .overlay(overlay)
                .light(light)
                .normal(nx, ny, nz);
    }

    /**
     * 根据世界Y坐标计算纹理V坐标，实现垂直条纹效果
     * 纹理V坐标基于方块的世界Y坐标 + 顶点在方块内的局部Y坐标
     * 这样无论方块如何旋转，条纹都始终基于世界垂直方向
     */
    private float getVForWorldY(BlockPos blockPos, float localY) {
        float worldY = blockPos.getY() + localY;
        float v = worldY - (float)Math.floor(worldY);
        return v;
    }

    private Identifier getTextureForType(CoilType type) {
        return switch (type) {
            case COPPER -> TEXTURE_COPPER;
            case PIG_IRON -> TEXTURE_PIG_IRON;
            case NEKO_COPPER -> TEXTURE_NEKO_COPPER;
            default -> TEXTURE_COPPER;
        };
    }

    /**
     * 渲染附着在方块六个面上的零件
     */
    private void renderAttachedComponents(CoilBlockEntity blockEntity, MatrixStack matrices,
                                          VertexConsumerProvider vertexConsumers, int light, int overlay) {
        // 检查方块实体是否实现了ComponentAdaptation接口
        if (!(blockEntity instanceof ComponentAdaptation componentAdaptation)) {
            return;
        }

        if (componentAdaptation.getAttachedComponents().isEmpty()) {
            return;
        }

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
    private int calculateFaceLight(CoilBlockEntity blockEntity, Direction side) {
        if (blockEntity.getWorld() == null) {
            return LightmapTextureManager.MAX_LIGHT_COORDINATE;
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

        matrices.translate(0.5f, 0.5f, 0.5f);

        switch (side) {
            case DOWN -> matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
            case UP -> matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90));
            case NORTH -> matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180));
            case SOUTH -> {}
            case WEST -> matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-90));
            case EAST -> matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90));
        }

        matrices.translate(0, 0, 0.1f);
        matrices.scale(0.9f, 0.9f, 0.9f);

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

    private void renderFixationFrame(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay, BlockEntity entity) {
        if (entity.getWorld() == null) return;

        MinecraftClient client = MinecraftClient.getInstance();

        net.minecraft.item.Item frameItem = ModItems.pig_iron_framework;
        if (frameItem == null) {
            NekoTechnology.LOGGER.warn("Pig iron framework item not registered");
            return;
        }

        ItemStack frameStack = new ItemStack(frameItem, 1);

        matrices.push();

        matrices.translate(0.5f, 0.5f, 0.5f);

        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));

        matrices.scale(1.9f, 1.9f, 1.9f);

        client.getItemRenderer().renderItem(
                frameStack,
                ModelTransformationMode.FIXED,
                light,
                overlay,
                matrices,
                vertexConsumers,
                client.world,
                0
        );

        matrices.pop();
    }
}

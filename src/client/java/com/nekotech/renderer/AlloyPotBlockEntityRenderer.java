package com.nekotech.renderer;

import com.nekotech.block.entity.machines.AlloyPotBlockEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

@Environment(EnvType.CLIENT)
public class AlloyPotBlockEntityRenderer implements BlockEntityRenderer<AlloyPotBlockEntity> {

    private final ItemRenderer itemRenderer;

    private static final int[] SLOTS = {
            AlloyPotBlockEntity.INPUT_SLOT_1,
            AlloyPotBlockEntity.INPUT_SLOT_2,
            AlloyPotBlockEntity.OUTPUT_SLOT_1,
            AlloyPotBlockEntity.OUTPUT_SLOT_2
    };

    private static final float BASE_RADIUS = 0.08f;
    private static final float RADIUS_VARIATION = 0.04f;
    private static final float BASE_HEIGHT = 0.32f;
    private static final int FULL_BRIGHT = 0x00F000F0;

    public AlloyPotBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
        this.itemRenderer = MinecraftClient.getInstance().getItemRenderer();
    }

    @Override
    public void render(AlloyPotBlockEntity entity, float tickDelta,
                       MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                       int light, int overlay) {

        World world = entity.getWorld();
        if (world == null) return;

        long baseSeed = entity.getPos().asLong();
        float time = world.getTime() + tickDelta;

        for (int slot : SLOTS) {
            ItemStack stack = entity.getStack(slot);
            if (stack.isEmpty()) continue;

            long seed = computeSeed(baseSeed, stack, slot);

            float xOffset = extractFloat(seed, 0) * 0.2f;
            float zOffset = extractFloat(seed, 16) * 0.2f;

            renderItem(entity, stack,
                    seed, time,
                    xOffset, zOffset,
                    matrices, vertexConsumers);
        }
    }

    private void renderItem(AlloyPotBlockEntity be, ItemStack stack,
                            long seed, float time,
                            float xOffset, float zOffset,
                            MatrixStack matrices, VertexConsumerProvider vcp) {

        matrices.push();

        boolean working = be.alloyProgress > 0;

        if (working) {

            // ===== 一次 seed → 多随机 =====
            float randOrbit = extract01(seed, 0);
            float randSpin  = extract01(seed, 16);
            float randPhase = extract01(seed, 32);

            float active = MathHelper.clamp(
                    be.alloyProgress / (float)be.getAlloyTimeTotal(), 0f, 1f
            );

            float radius = (BASE_RADIUS + randOrbit * RADIUS_VARIATION) * active;
            float speed = (4f + randOrbit * 4f) * active;
            float spinSpeed = 4f + randSpin * 10f;

            float angle = time * speed + randPhase * 360f;
            float rad = angle * MathHelper.RADIANS_PER_DEGREE;

            float cos = MathHelper.cos(rad);
            float sin = MathHelper.sin(rad);

            float x = xOffset + cos * radius;
            float z = zOffset + sin * radius;

            float baseHeight = BASE_HEIGHT + randOrbit * 0.06f;
            float y = baseHeight + MathHelper.sin(rad * 2f) * 0.05f;

            matrices.translate(0.5f, 0.25f, 0.5f);

            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((time * 8f) % 360f));

            matrices.translate(x, y, z);

            matrices.scale(0.5f, 0.5f, 0.5f);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90f));

            matrices.multiply(
                    RotationAxis.POSITIVE_Y.rotationDegrees((time * spinSpeed) % 360f)
            );

        } else {
            matrices.translate(
                    0.5f + xOffset,
                    0.25f,
                    0.5f + zOffset
            );

            matrices.scale(0.5f, 0.5f, 0.5f);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90f));
        }

        itemRenderer.renderItem(
                stack,
                ModelTransformationMode.FIXED,
                FULL_BRIGHT,
                OverlayTexture.DEFAULT_UV,
                matrices,
                vcp,
                be.getWorld(),
                0
        );

        matrices.pop();
    }

    // ===== 轻量 hash =====
    private long computeSeed(long baseSeed, ItemStack stack, int slot) {
        long seed = baseSeed;
        seed ^= stack.getItem().hashCode() * 31L;
        seed ^= slot * 1664525L;
        seed = seed * 1664525L + 1013904223L;
        return seed;
    }

    // ===== [-1,1] =====
    private float extractFloat(long seed, int shift) {
        int bits = (int)((seed >> shift) & 0xFFFF);
        float normalized = bits / 65535f;
        return normalized * 2f - 1f;
    }

    // ===== [0,1] =====
    private float extract01(long seed, int shift) {
        int bits = (int)((seed >> shift) & 0xFFFF);
        return bits / 65535f;
    }
}
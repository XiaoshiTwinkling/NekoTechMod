package com.nekotech.renderer.blockentities;

import com.nekotech.block.custom.ChargeStationBlock;
import com.nekotech.block.entity.machines.ChargeStationBlockEntity;
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

public class ChargeStationBlockEntityRenderer implements BlockEntityRenderer<ChargeStationBlockEntity> {
    public ChargeStationBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
    }

    @Override
    public void render(ChargeStationBlockEntity entity, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {

        ItemStack stack = entity.getStack(ChargeStationBlockEntity.SLOT);
        if (stack.isEmpty()) {
            return;
        }

        Direction facing = entity.getCachedState().get(ChargeStationBlock.FACING);
        float rotationAngle = -facing.asRotation();

        matrices.push();

        matrices.translate(0.5, 0.5080, 0.5);

        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotationAngle));

        matrices.translate(0.0, 0.06, 0.0);

        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90f));

        float scale = 0.4f;
        matrices.scale(scale, scale, scale);

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

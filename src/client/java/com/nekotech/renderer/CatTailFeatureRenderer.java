package com.nekotech.renderer;

import com.nekotech.item.ModItems;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.entity.EquipmentSlot;

public class CatTailFeatureRenderer extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {

    private final TailRenderer tail = new TailRenderer();

    public CatTailFeatureRenderer(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrices,
                       VertexConsumerProvider vertices,
                       int light,
                       AbstractClientPlayerEntity player,
                       float limbAngle,
                       float limbDistance,
                       float tickDelta,
                       float age,
                       float headYaw,
                       float headPitch) {


        if (!hasTail(player)) return;

        matrices.push();

        // 绑定身体（关键）
        this.getContextModel().body.rotate(matrices);

        // 腰部位置（裤子）
        matrices.translate(0.0, 0.6, 0.15);

        tail.render(matrices, vertices, light, player, age, limbAngle, limbDistance);

        matrices.pop();
    }

    private boolean hasTail(AbstractClientPlayerEntity player) {
        return player.getEquippedStack(EquipmentSlot.LEGS)
                .isOf(ModItems.NEKO_TAIL);
    }
}

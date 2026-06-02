package com.nekotech.renderer;

import com.nekotech.item.ModItems;
import com.nekotech.item.custom.NekoMark.NekoMarkAccess;
import com.nekotech.mixin.client.OcelotEntityModelAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.CatEntityModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.RotationAxis;

public class NekoMarkFeatureRenderer extends FeatureRenderer<CatEntity, CatEntityModel<CatEntity>> {

    public NekoMarkFeatureRenderer(FeatureRendererContext<CatEntity, CatEntityModel<CatEntity>> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers,
                       int light,
                       CatEntity cat,
                       float limbAngle,
                       float limbDistance,
                       float tickDelta,
                       float age,
                       float headYaw,
                       float headPitch) {
        if (!(cat instanceof NekoMarkAccess access)) {
            return;
        }

        DyeColor color = access.neko_technology$getNekoMarkColor();

        if (color == null) {
            return;
        }

        matrices.push();

        ((OcelotEntityModelAccessor) this.getContextModel())
                .neko_technology$getBody()
                .rotate(matrices);

// vanilla cat/ocelot body 的局部 cuboid 大致是：
// x: -2..2
// y:  3..19   ← 身体长度方向，y = 3 是朝前面
// z: -8..-2
//
// MatrixStack 单位是 1 block = 16 model pixels，
// 所以这里用 /16。
        matrices.translate(
                0.0F,                 // x 中心
                2.3F / 16.0F - 0.01F, // 贴到前面，并稍微向外推，避免 z-fighting
                -6.0F / 16.0F         // z 高度中心：(-8 + -2) / 2 = -5
        );

// 把物品平面从默认朝向旋到 body 前面这个平面上
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180.0F));



        matrices.scale(0.22F, 0.22F, 0.22F);

        MinecraftClient.getInstance().getItemRenderer().renderItem(
                new ItemStack(getMarkItem(color)),
                ModelTransformationMode.FIXED,
                light,
                OverlayTexture.DEFAULT_UV,
                matrices,
                vertexConsumers,
                cat.getWorld(),
                cat.getId()
        );

        matrices.pop();
    }

    private static Item getMarkItem(DyeColor color) {
        return switch (color) {
            case WHITE -> ModItems.WHITE_NEKO_MARK;
            case ORANGE -> ModItems.ORANGE_NEKO_MARK;
            case MAGENTA -> ModItems.MAGENTA_NEKO_MARK;
            case LIGHT_BLUE -> ModItems.LIGHT_BLUE_NEKO_MARK;
            case YELLOW -> ModItems.YELLOW_NEKO_MARK;
            case LIME -> ModItems.LIME_NEKO_MARK;
            case PINK -> ModItems.PINK_NEKO_MARK;
            case GRAY -> ModItems.GRAY_NEKO_MARK;
            case LIGHT_GRAY -> ModItems.LIGHT_GRAY_NEKO_MARK;
            case CYAN -> ModItems.CYAN_NEKO_MARK;
            case PURPLE -> ModItems.PURPLE_NEKO_MARK;
            case BLUE -> ModItems.BLUE_NEKO_MARK;
            case BROWN -> ModItems.BROWN_NEKO_MARK;
            case GREEN -> ModItems.GREEN_NEKO_MARK;
            case RED -> ModItems.RED_NEKO_MARK;
            case BLACK -> ModItems.BLACK_NEKO_MARK;
        };
    }
}

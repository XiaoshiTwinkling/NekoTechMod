package com.nekotech.renderer;


import com.nekotech.block.entity.machines.api.ComponentAdaptation;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;

/**
 * 用于渲染附着在方块六个面上的零件的通用渲染器
 */
public class GenericComponentRenderer{
    public static void render(
            BlockEntity blockEntity,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            int overlay
    ) {
        // 检查方块实体是否支持零件
        if (!(blockEntity instanceof ComponentAdaptation machine)) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        ItemRenderer itemRenderer = client.getItemRenderer();

        // 遍历六个面
        for (Direction direction : Direction.values()) {
            ItemStack componentStack = machine.getAttachedComponents().get(direction).getDefaultStack();

            if (componentStack.isEmpty()) {
                continue;
            }

            matrices.push();

            /*
             * 处理复杂的矩阵变换
             */
            itemRenderer.renderItem(
                    componentStack,
                    ModelTransformationMode.FIXED, // 固定模式，适合贴在方块上
                    false, // 是否左撇子
                    matrices,
                    vertexConsumers,
                    light,
                    overlay,
                    itemRenderer.getModel(componentStack, null, null, 0)
            );

            matrices.pop();
        }
    }
}

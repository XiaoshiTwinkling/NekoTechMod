package com.nekotech.client.hud;

import com.nekotech.NekoTechnology;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

public abstract class GoogleAbstractHUDClient {
    protected int x = 0;
    protected int y = 0;
    protected int width = 176;
    protected int height = 166;
    protected static final int ITEM_SLOT_SIZE = 18;

    /**
     * 渲染HUD
     */
    public abstract void render(DrawContext context, float tickDelta);

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    // 客户端工具方法
    protected void drawVanillaBackground(DrawContext context, Identifier texture) {
        int border = 4;
        int textureWidth = 256;
        int textureHeight = 256;

        // 绘制四个角
        context.drawTexture(texture, x, y, 0, 0, border, border);
        context.drawTexture(texture, x + width - border, y, textureWidth - border, 0, border, border);
        context.drawTexture(texture, x, y + height - border, 0, textureHeight - border, border, border);
        context.drawTexture(texture, x + width - border, y + height - border,
                textureWidth - border, textureHeight - border, border, border);

        // 绘制四条边
        context.drawTexture(texture, x + border, y, border, 0, width - 2 * border, border);
        context.drawTexture(texture, x + border, y + height - border, border, textureHeight - border,
                width - 2 * border, border);
        context.drawTexture(texture, x, y + border, 0, border, border, height - 2 * border);
        context.drawTexture(texture, x + width - border, y + border, textureWidth - border, border,
                border, height - 2 * border);

        // 绘制中间区域
        context.drawTexture(texture, x + border, y + border, border, border,
                width - 2 * border, height - 2 * border);
    }

    protected void drawTextWithShadow(DrawContext context, Text text, int x, int y, int color) {
        context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, text, x, y, color);
    }

    protected void drawItemSlot(DrawContext context, int x, int y, ItemStack stack) {
        Identifier SLOT_TEXTURE = Identifier.ofVanilla("container/slot");
        context.drawGuiTexture(SLOT_TEXTURE, x, y, 18, 18);
        if (!stack.isEmpty()) {
            // 绘制物品图标（居中）
            context.drawItem(stack, x + 1, y + 1);

            // 绘制物品数量（如果有）
            int count = stack.getCount();
            if (count > 1) {
                String countText = String.valueOf(count);
                var textRenderer = MinecraftClient.getInstance().textRenderer;
                int textWidth = textRenderer.getWidth(countText);
                int textX = x + 18 - textWidth - 1;
                int textY = y + 18 - 8;
                context.drawText(textRenderer, countText, textX, textY, 0xFFFFFF, true);
            }
        }
    }

    public void renderInventory(DrawContext context, int inventoryStartX, int inventoryStartY, int rows, int columns, List<ItemStack> items) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                // 计算当前槽位的屏幕坐标
                int slotX = inventoryStartX + col * 18; // 每个槽位宽度 18
                int slotY = inventoryStartY + row * 18; // 每个槽位高度 18

                // 计算当前槽位的索引（从 0 开始）
                int slotIndex = row * columns + col;

                // 获取当前槽位的物品栈（确保 items 不为空且索引有效）
                ItemStack stack = ItemStack.EMPTY; // 默认为空
                if (items != null && slotIndex < items.size()) {
                    stack = items.get(slotIndex);
                }

                // 调用 drawItemSlot 绘制当前槽位
                drawItemSlot(context, slotX, slotY, stack);
            }
        }
    }
}

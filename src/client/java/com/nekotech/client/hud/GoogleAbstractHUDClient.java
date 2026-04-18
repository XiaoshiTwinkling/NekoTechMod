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
            var matrices = context.getMatrices();
            matrices.push();
            matrices.translate(0, 0, 200);

            int count = stack.getCount();
            if (count > 1) {
                String countText = String.valueOf(count);
                var textRenderer = MinecraftClient.getInstance().textRenderer;
                int textX = x + 18 - textRenderer.getWidth(countText) - 1;
                int textY = y + 18 - 8;
                context.drawText(textRenderer, countText, textX, textY, 0xFFFFFF, true);
            }

            matrices.pop();
            context.drawItem(stack, x + 1, y + 1);
        }
    }

    public void renderInventory(DrawContext context, int inventoryStartX, int inventoryStartY, int rows, int columns, List<ItemStack> items) {
        if (items == null) {
            return;
        }

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                int slotX = inventoryStartX + col * 18;
                int slotY = inventoryStartY + row * 18;

                int slotIndex = row * columns + col;
                ItemStack stack = ItemStack.EMPTY;

                if (slotIndex < items.size()) {
                    stack = items.get(slotIndex);
                }

                drawItemSlot(context, slotX, slotY, stack);
            }
        }
    }
    public abstract void update(com.nekotech.item.api.googles.GoogleAbstractHUD data);

    public abstract boolean isSame(com.nekotech.item.api.googles.GoogleAbstractHUD data);
}

package com.nekotech.client.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

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

    protected void drawItemSlot(DrawContext context, int x, int y) {
        context.drawTexture(Identifier.of("textures/gui/container/generic_54.png"),
                x, y, 0, 0, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE);
    }
}

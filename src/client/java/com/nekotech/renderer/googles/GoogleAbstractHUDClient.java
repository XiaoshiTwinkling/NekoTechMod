package com.nekotech.renderer.googles;

import com.mojang.blaze3d.systems.RenderSystem;
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

    /**
     * HUD定位喵~~~~
     */
    public enum HudPosition {
        // 四个角落
        TOP_RIGHT,      // 右上角
        TOP_LEFT,       // 左上角
        BOTTOM_RIGHT,   // 右下角
        BOTTOM_LEFT,    // 左下角

        // 四条边中间
        LEFT_CENTER,    // 左中
        RIGHT_CENTER,   // 右中
        TOP_CENTER,     // 上中
        BOTTOM_CENTER,  // 下中

        // 中心相关
        CENTER,         // 正中心
        CENTER_LEFT,    // 中间稍微偏左
        CENTER_RIGHT,   // 中间稍微偏右
        CENTER_UP,      // 中间稍微偏上
        CENTER_DOWN,    // 中间稍微偏下

        // 特殊位置
        SCREEN_RIGHT,   // 屏幕右侧（垂直居中）
        SCREEN_LEFT,    // 屏幕左侧（垂直居中）
        CUSTOM          // 自定义位置
    }

    /**
     * 获取HUD的默认位置类型喵~~~~
     * 每个HUD子类必须实现此方法来定义自己的默认位置喵~~~~
     */
    public abstract HudPosition getDefaultPosition();

    /**
     * 根据位置类型和屏幕尺寸计算实际坐标喵~~~~
     * @param screenWidth 屏幕宽度
     * @param screenHeight 屏幕高度
     * @param margin 边距（像素）
     */
    public void calculatePosition(int screenWidth, int screenHeight, int margin) {
        // 确保边距不小于0
        margin = Math.max(0, margin);

        switch (getDefaultPosition()) {
            // 四个角落
            case TOP_RIGHT:
                this.x = screenWidth - this.width - margin;
                this.y = margin;
                break;
            case TOP_LEFT:
                this.x = margin;
                this.y = margin;
                break;
            case BOTTOM_RIGHT:
                this.x = screenWidth - this.width - margin;
                this.y = screenHeight - this.height - margin;
                break;
            case BOTTOM_LEFT:
                this.x = margin;
                this.y = screenHeight - this.height - margin;
                break;

            // 四条边中间
            case LEFT_CENTER:  // 左中
                this.x = margin;
                this.y = (screenHeight - this.height) / 2;
                break;
            case RIGHT_CENTER: // 右中
                this.x = screenWidth - this.width - margin;
                this.y = (screenHeight - this.height) / 2;
                break;
            case TOP_CENTER:   // 上中
                this.x = (screenWidth - this.width) / 2;
                this.y = margin;
                break;
            case BOTTOM_CENTER: // 下中
                this.x = (screenWidth - this.width) / 2;
                this.y = screenHeight - this.height - margin;
                break;

            // 中心相关
            case CENTER:  // 正中心
                this.x = (screenWidth - this.width) / 2;
                this.y = (screenHeight - this.height) / 2;
                break;
            case CENTER_LEFT:  // 中间稍微偏左
                this.x = (screenWidth - this.width) / 4 - (this.width / 4);
                this.y = (screenHeight - this.height) / 2;
                break;
            case CENTER_RIGHT:  // 中间稍微偏右
                this.x = (screenWidth - this.width) / 4  + (this.width / 4);
                this.y = (screenHeight - this.height) / 2;
                break;
            case CENTER_UP:  // 中间稍微偏上
                this.x = (screenWidth - this.width) / 2;
                this.y = (screenHeight - this.height) / 4 - (this.height / 4);
                break;
            case CENTER_DOWN:  // 中间稍微偏下
                this.x = (screenWidth - this.width) / 2;
                this.y = (screenHeight - this.height) / 4  + (this.height / 4);
                break;

            // 特殊位置
            case SCREEN_RIGHT:  // 屏幕右侧（垂直居中）
                this.x = screenWidth - this.width - margin;
                this.y = (screenHeight - this.height) / 2;
                break;
            case SCREEN_LEFT:  // 屏幕左侧（垂直居中）
                this.x = margin;
                this.y = (screenHeight - this.height) / 2;
                break;
            case CUSTOM:
                calculateCustomPosition(screenWidth, screenHeight, margin);
                break;
        }
        ensureWithinScreenBounds(screenWidth, screenHeight, margin);
    }

    /**
     * 确保HUD不超出屏幕边界
     */
    protected void ensureWithinScreenBounds(int screenWidth, int screenHeight, int margin) {
        this.x = Math.max(margin, Math.min(this.x, screenWidth - this.width - margin));

        this.y = Math.max(margin, Math.min(this.y, screenHeight - this.height - margin));

        if (this.width > screenWidth - 2 * margin) {
            this.width = screenWidth - 2 * margin;
        }
        if (this.height > screenHeight - 2 * margin) {
            this.height = screenHeight - 2 * margin;
        }
    }

    /**
     * 新增：自定义位置计算（可被子类重写）
     */
    protected void calculateCustomPosition(int screenWidth, int screenHeight, int margin) {
        // 默认实现：使用右上角
        this.x = screenWidth - this.width - margin;
        this.y = margin;
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

        context.setShaderColor(1.0f, 1.0f, 1.0f, 0.6f);
        var matrices_bg = context.getMatrices();
        matrices_bg.push();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        context.drawGuiTexture(SLOT_TEXTURE, x, y, 18, 18);

        context.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
        matrices_bg.pop();

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

package com.nekotech.renderer.googles.templates;

import com.mojang.blaze3d.systems.RenderSystem;
import com.nekotech.renderer.googles.GoogleAbstractHUDClient;
import com.nekotech.item.api.googles.GoogleAbstractHUD;
import com.nekotech.item.api.googles.templates.InfoBoxHUDData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Map;

public class InfoBoxHUDClient extends GoogleAbstractHUDClient {
    private InfoBoxHUDData hudData;
    private List<OrderedText> wrappedContentLines = null;
    private int contentHeight = 0;

    public InfoBoxHUDClient(InfoBoxHUDData hudData) {
        this.hudData = hudData;

        // 设置位置和尺寸
        this.width = hudData.getWidth();
        this.height = hudData.getHeight();

        // 计算文本换行和高度
        calculateTextLayout();
    }

    private void calculateTextLayout() {
        if (hudData == null) return;

        Map<String, Object> data = hudData.getRenderData();
        Text title = (Text) data.get("title");
        Text content = (Text) data.get("content");
        int maxWidth = (int) data.get("maxWidth");

        var textRenderer = MinecraftClient.getInstance().textRenderer;
        int padding = 8; // 内边距
        int titleBottomMargin = 4; // 标题和内容间距
        int lineSpacing = 1; // 行间距

        // 计算可用宽度
        int availableWidth = maxWidth - 2 * padding;

        // 计算标题高度
        int titleHeight = 0;
        if (title != null) {
            // 标题可能也需要换行
            List<OrderedText> titleLines = textRenderer.wrapLines(title, availableWidth);
            titleHeight = titleLines.size() * 9 + 2; // 每行9像素，加2像素间距
        }

        // 计算内容高度
        int contentHeight = 0;
        List<OrderedText> contentLines = null;
        if (content != null) {
            contentLines = textRenderer.wrapLines(content, availableWidth);
            contentHeight = contentLines.size() * 9;
        }

        // 计算总高度
        int totalHeight = padding * 2; // 上下内边距
        if (title != null) {
            totalHeight += titleHeight + titleBottomMargin;
        }
        if (content != null) {
            totalHeight += contentHeight;
        }

        // 确保最小高度
        this.height = Math.max(totalHeight, 60);
        this.wrappedContentLines = contentLines;
        this.contentHeight = contentHeight;
    }

    @Override
    public void render(DrawContext context, float tickDelta) {
        if (hudData == null) return;

        Map<String, Object> data = hudData.getRenderData();
        Text title = (Text) data.get("title");
        Text content = (Text) data.get("content");
        var textRenderer = MinecraftClient.getInstance().textRenderer;

        // 1. 绘制完整背景
        drawCompleteBackground(context, x, y, width, height);

        int padding = 8;
        int currentY = y + padding;

        // 2. 绘制标题（支持多行）
        if (title != null) {
            List<OrderedText> titleLines = textRenderer.wrapLines(title, width - 2 * padding);
            for (int i = 0; i < titleLines.size(); i++) {
                OrderedText line = titleLines.get(i);
                int lineWidth = textRenderer.getWidth(line);
                int titleX = x + (width - lineWidth) / 2; // 居中
                int titleY = currentY + i * 9;
                context.drawText(textRenderer, line, titleX, titleY, 0x404040, true);
            }
            currentY += titleLines.size() * 9 + 4;
        }

        // 3. 绘制内容
        if (content != null && wrappedContentLines != null) {
            int contentX = x + padding;
            int contentY = currentY;

            for (int i = 0; i < wrappedContentLines.size(); i++) {
                // 确保不会超出HUD底部
                if (contentY + i * 9 > y + height - padding) {
                    break; // 停止绘制，避免超出
                }

                OrderedText line = wrappedContentLines.get(i);
                context.drawText(textRenderer, line, contentX, contentY + i * 9, 0xFFFFFF, true);
            }
        }
    }

    @Override
    public HudPosition getDefaultPosition() {
        return HudPosition.CENTER_LEFT;
    }

    @Override
    public void update(GoogleAbstractHUD data) {
        if (data instanceof InfoBoxHUDData newData) {
            this.hudData = newData;

            this.width = newData.getWidth();
            this.height = newData.getHeight();

            calculateTextLayout();
        }
    }

    @Override
    public boolean isSame(GoogleAbstractHUD data) {
        if (!(data instanceof InfoBoxHUDData other)) return false;

        return this.hudData.getPos().equals(other.getPos());
    }

    /**
     * 绘制完整的背景，确保边缘不被裁剪
     */
    protected void drawCompleteBackground(DrawContext context, int x, int y, int width, int height) {
        // 使用原版对话框背景纹理
        final Identifier DIALOG_TEXTURE = Identifier.of("textures/gui/demo_background.png");
        int border = 4;
        int textureWidth = 248;
        int textureHeight = 166;

        context.setShaderColor(1.0f, 1.0f, 1.0f, 0.6f);

        var matrices = context.getMatrices();
        matrices.push();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // 绘制四个角
        context.drawTexture(DIALOG_TEXTURE, x, y, 0, 0, border, border); // 左上
        context.drawTexture(DIALOG_TEXTURE, x + width - border, y, textureWidth - border, 0, border, border); // 右上
        context.drawTexture(DIALOG_TEXTURE, x, y + height - border, 0, textureHeight - border, border, border); // 左下
        context.drawTexture(DIALOG_TEXTURE, x + width - border, y + height - border,
                textureWidth - border, textureHeight - border, border, border); // 右下

        // 绘制四条边
        context.drawTexture(DIALOG_TEXTURE, x + border, y, border, 0, width - 2 * border, border); // 上
        context.drawTexture(DIALOG_TEXTURE, x + border, y + height - border, border, textureHeight - border,
                width - 2 * border, border); // 下
        context.drawTexture(DIALOG_TEXTURE, x, y + border, 0, border, border, height - 2 * border); // 左
        context.drawTexture(DIALOG_TEXTURE, x + width - border, y + border, textureWidth - border, border,
                border, height - 2 * border); // 右

        // 绘制中间区域
        context.drawTexture(DIALOG_TEXTURE, x + border, y + border, border, border,
                width - 2 * border, height - 2 * border);

        context.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
        matrices.pop();
    }

    public int getContentHeight() {
        return contentHeight;
    }
}

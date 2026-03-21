package com.nekotech.screen;


import com.mojang.blaze3d.systems.RenderSystem;
import com.nekotech.NekoTechnology;
import com.nekotech.screen.AlloyFurnaceScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;


public class AlloyFurnaceScreen extends HandledScreen<AlloyFurnaceScreenHandler> {

    private static final Identifier TEXTURE = Identifier.of(NekoTechnology.MOD_ID, "textures/gui/basic_alloy_furnace_gui.png");

    public AlloyFurnaceScreen(AlloyFurnaceScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        context.drawTexture(TEXTURE, x, y, 0, 0, backgroundWidth, backgroundHeight);

        renderProgressArrow(context, x, y);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }

    private void renderProgressArrow(DrawContext context, int x, int y) {
        if (handler.isCrafting()) {
            int flameWidth = 13;
            int flameHeight = 13;

            // 获取当前进度（0-13，因为火焰高度为14）
            int progress = handler.getScaledProgress();

            if (progress > 0) {
                // 火焰底部固定位置
                int flameBottomY = y + 33 + flameHeight;  // 火焰底部Y坐标

                // 计算实际应该渲染的火焰高度
                int currentFlameHeight = Math.min(progress, flameHeight);

                // 计算火焰顶部Y坐标（从下往上渲染）
                int flameTopY = flameBottomY - currentFlameHeight;

                // 计算纹理V坐标偏移
                int textureVOffset = flameHeight - currentFlameHeight;

                context.drawTexture(TEXTURE,
                        x + 82,                     // 屏幕X坐标
                        flameTopY,                  // 屏幕Y坐标（从下往上计算）
                        176,                       // 纹理U坐标
                        0 + textureVOffset,        // 纹理V坐标偏移
                        flameWidth,                // 纹理宽度
                        currentFlameHeight,        // 渲染高度
                        256, 256                   // 纹理图尺寸
                );
            }
        }
    }
}

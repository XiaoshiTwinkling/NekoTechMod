package com.nekotech.client.hud.templates;

import com.nekotech.client.hud.GoogleAbstractHUDClient;
import com.nekotech.item.api.googles.templates.ContainerHUDData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Map;

public class ContainerItemsHUDClient extends GoogleAbstractHUDClient {
    private final ContainerHUDData hudData;
    private final Identifier backgroundTexture;

    public ContainerItemsHUDClient(ContainerHUDData hudData) {
        this.hudData = hudData;
        this.backgroundTexture = Identifier.of("textures/gui/container/generic_54.png");

        // 从数据类获取位置和尺寸
        this.x = hudData.getX();
        this.y = hudData.getY();
        this.width = hudData.getWidth();
        this.height = hudData.getHeight();
    }

    @Override
    public void render(DrawContext context, float tickDelta) {
        if (hudData == null) return;

        Map<String, Object> data = hudData.getRenderData();
        List<ItemStack> items = (List<ItemStack>) data.get("items");
        int columns = (int) data.get("columns");
        int rows = (int) data.get("rows");
        Text title = hudData.getTitle();

        if (items == null || items.isEmpty()) return;

        // 1. 绘制背景
        drawVanillaBackground(context, backgroundTexture);

        // 2. 绘制标题
        if (title != null) {
            int titleX = x + (width - MinecraftClient.getInstance().textRenderer.getWidth(title)) / 2;
            int titleY = y + 6;
            drawTextWithShadow(context, title, titleX, titleY, 0x404040);
        }

        // 3. 计算物品栏起始位置
        int inventoryStartX = x + 8;
        int inventoryStartY = y + 18;

        // 4. 绘制物品栏格子
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                int slotX = inventoryStartX + col * ITEM_SLOT_SIZE;
                int slotY = inventoryStartY + row * ITEM_SLOT_SIZE;

                // 绘制格子背景
                drawItemSlot(context, slotX, slotY);

                // 绘制物品
                int slotIndex = row * columns + col;
                if (slotIndex < items.size()) {
                    ItemStack stack = items.get(slotIndex);
                    if (!stack.isEmpty()) {
                        context.drawItem(stack, slotX + 1, slotY + 1);
                        context.drawItemInSlot(MinecraftClient.getInstance().textRenderer, stack, slotX + 1, slotY + 1);
                    }
                }
            }
        }

        // 5. 绘制物品总数
        int totalItems = (int) data.get("totalItems");
        Text countText = Text.translatable("hud.container.total_items", totalItems);
        int countX = x + 8;
        int countY = y + height - 20;
        drawTextWithShadow(context, countText, countX, countY, 0x404040);
    }
}

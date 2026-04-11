package com.nekotech.client.hud.templates;

import com.nekotech.NekoTechnology;
import com.nekotech.client.hud.GoogleAbstractHUDClient;
import com.nekotech.item.api.googles.templates.ContainerHUDData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;

import java.util.HashMap;
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

        HashMap<String, Object> data = hudData.getRenderData();
        List<ItemStack> items = (List<ItemStack>) data.get("items");
        int columns = (int) data.get("columns");
        int rows = (int) data.get("rows");
        Text title = hudData.getTitle();

        if (title != null) {
            int titleX = x + (width - MinecraftClient.getInstance().textRenderer.getWidth(title)) / 2;
            int titleY = y + 6;
            // 使用原版标题颜色（白色带黑色阴影）
            drawTextWithShadow(context, title, titleX, titleY, 0x404040);
        }

        renderInventory(context, x, y ,rows, columns, items);
    }

}

package com.nekotech.renderer.googles.templates;

import com.nekotech.renderer.googles.GoogleAbstractHUDClient;
import com.nekotech.item.api.googles.GoogleAbstractHUD;
import com.nekotech.item.api.googles.templates.ContainerHUDData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Map;

public class ContainerItemsHUDClient extends GoogleAbstractHUDClient {
    private ContainerHUDData hudData;
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

        if (title != null) {
            int titleX = x + (width - MinecraftClient.getInstance().textRenderer.getWidth(title)) / 2;
            int titleY = y + 6 + 15;
            drawTextWithShadow(context, title, titleX, titleY, 0x404040);
        }

        int inventoryStartX = x + 8;
        int inventoryStartY = y + (title != null ? 20 : 8) + 15;

        renderInventory(context, inventoryStartX, inventoryStartY, rows, columns, items);
    }

    @Override
    public HudPosition getDefaultPosition() {
        return HudPosition.TOP_RIGHT;
    }

    @Override
    public void update(com.nekotech.item.api.googles.GoogleAbstractHUD data) {
        if (data instanceof ContainerHUDData containerData) {
            this.hudData = containerData;
            this.width = containerData.getWidth();
            this.height = containerData.getHeight();
        }
    }

    @Override
    public boolean isSame(GoogleAbstractHUD data) {
        if (!(data instanceof ContainerHUDData other)) return false;

        return this.hudData.getPos().equals(other.getPos());
    }

}

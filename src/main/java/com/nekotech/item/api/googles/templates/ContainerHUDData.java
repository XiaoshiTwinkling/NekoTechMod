package com.nekotech.item.api.googles.templates;

import com.nekotech.NekoTechnology;
import com.nekotech.item.api.googles.GoogleAbstractHUD;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContainerHUDData extends GoogleAbstractHUD {
    private final List<ItemStack> items;
    private final Text title;
    private final int columns;
    private final int rows;

    public ContainerHUDData(BlockPos pos, List<ItemStack> items, Text title, int columns, int rows) {
        super(pos);
        this.items = items;
        this.title = title;
        this.columns = columns;
        this.rows = rows;
        int titleHeight = title != null ? 20 : 6;
        int bottomHeight = 20;

        this.width = 14 + columns * ITEM_SLOT_SIZE;  // 原版是7+7+格子宽度
        this.height = titleHeight + 14 + rows * ITEM_SLOT_SIZE + bottomHeight;
    }

    @Override
    public String getType() {
        return "container";
    }

    @Override
    public @Nullable Text getTitle() {
        return title;
    }

    @Override
    public Map<String, Object> getRenderData() {

        Map<String, Object> data = new HashMap<>();

        List<ItemStack> renderItems = new ArrayList<>();
        for (ItemStack stack : this.items) {
            if (stack == null) {
                renderItems.add(ItemStack.EMPTY);
            } else {
                renderItems.add(stack.copy());  // 创建副本
            }
        }

        data.put("items", renderItems);
        data.put("columns", this.columns);
        data.put("rows", this.rows);

        return data;
    }

    public int getColumns() { return columns; }
    public int getRows() { return rows; }
    public List<ItemStack> getItems() { return items; }
}

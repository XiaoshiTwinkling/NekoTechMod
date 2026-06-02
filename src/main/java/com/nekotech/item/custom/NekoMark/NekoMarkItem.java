package com.nekotech.item.custom.NekoMark;

import com.nekotech.item.ModItem;
import net.minecraft.util.DyeColor;

public class NekoMarkItem extends ModItem {
    private final DyeColor color;

    public NekoMarkItem(Settings settings, String name, DyeColor color) {
        super(settings, name);
        this.color = color;
    }

    public DyeColor getColor() {
        return this.color;
    }
}

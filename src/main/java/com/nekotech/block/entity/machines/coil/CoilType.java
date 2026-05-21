package com.nekotech.block.entity.machines.coil;

import com.nekotech.item.ModItems;
import net.minecraft.item.Item;

public enum CoilType {
    COPPER(ModItems.COPPER_COIL, 0xFFD700, "copper"),
    PIG_IRON(ModItems.PIG_IRON_COIL, 0xC0C0C0, "pig_iron"),
    NEKO_COPPER(ModItems.NEKO_COPPER_COIL, 0x8B4513, "neko_copper"),
    EMPTY(null, 0x000000, "empty");

    final Item item;
    final int color;
    final String id;

    CoilType(Item item, int color, String id) {
        this.item = item;
        this.color = color;
        this.id = id;
    }

    static CoilType fromItem(Item item) {
        for (CoilType type : values()) {
            if (type.item == item) return type;
        }
        return EMPTY;
    }
}

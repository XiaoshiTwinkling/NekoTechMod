package com.nekotech.util;

import com.nekotech.NekoTechnology;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;

public final class NekoTagTextures {
    private NekoTagTextures() {
    }

    public static Identifier background(String color) {
        DyeColor dyeColor = DyeColor.byName(color, DyeColor.WHITE);

        return Identifier.of(
                NekoTechnology.MOD_ID,
                "textures/gui/tag/" + dyeColor.getName() + ".png"
        );
    }
}
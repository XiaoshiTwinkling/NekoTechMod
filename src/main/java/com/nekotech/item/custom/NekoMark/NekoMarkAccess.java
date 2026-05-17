package com.nekotech.item.custom.NekoMark;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.DyeColor;
import org.jetbrains.annotations.Nullable;

public interface NekoMarkAccess {
    void neko_technology$setNekoMarkColor(DyeColor color);

    @Nullable
    DyeColor neko_technology$getNekoMarkColor();

    NbtCompound neko_technology$getNekoTaskData();
}
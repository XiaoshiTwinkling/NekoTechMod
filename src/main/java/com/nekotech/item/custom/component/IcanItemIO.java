package com.nekotech.item.custom.component;

import net.minecraft.item.ItemStack;

/**
 * 这个接口用来适配物品输出输出零件
 */
public interface IcanItemIO {
    default boolean canInsertwithComponent(ItemStack item){
        return true;
    }
}

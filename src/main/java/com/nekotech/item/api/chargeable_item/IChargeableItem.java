package com.nekotech.item.api.chargeable_item;

import net.minecraft.item.ItemStack;

/**
 * 可充能物品接口，实现此接口的物品可以在充能台上充能喵
 */
public interface IChargeableItem {

    /**
     * 获取物品的最大 NekoFlux 容量喵
     */
    float getMaxNekoFlux(ItemStack stack);

    /**
     * 获取物品当前的 NekoFlux 能量喵
     */
    float getNekoFlux(ItemStack stack);

    /**
     * 设置物品当前的 NekoFlux 能量喵
     */
    void setNekoFlux(ItemStack stack, float flux);

    /**
     * 检查物品是否可以使用喵
     */
    default boolean canUse(ItemStack stack) {
        return getNekoFlux(stack) > 0;
    }

    /**
     * 消耗指定数量的能量，如果不足则设为0喵
     */
    default void consumeNekoFlux(ItemStack stack, float amount) {
        float current = getNekoFlux(stack);
        setNekoFlux(stack, Math.max(0, current - amount));
    }
}
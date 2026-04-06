package com.nekotech.item.custom;

import net.minecraft.item.ItemStack;

public class GogglesItem extends HatItem{


    public GogglesItem(Type type, Settings settings) {
        super(type, settings);
    }

    public static boolean isWearingGoggles(net.minecraft.entity.player.PlayerEntity player) {
        ItemStack headStack = player.getInventory().getArmorStack(3);
        return headStack.getItem() instanceof GogglesItem;
    }
}

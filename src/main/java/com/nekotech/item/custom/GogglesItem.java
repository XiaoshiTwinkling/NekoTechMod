package com.nekotech.item.custom;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

public class GogglesItem extends HatItem{


    public GogglesItem(Type type, Settings settings) {
        super(type, settings);
    }

    // 检查玩家是否戴着猫猫护目镜喵~
    public static boolean isWearingCatGoggles(PlayerEntity player) {
        if (player == null) return false;

        ItemStack headSlot = player.getEquippedStack(EquipmentSlot.HEAD);
        return !headSlot.isEmpty() &&
                headSlot.getItem() instanceof GogglesItem;
    }
}

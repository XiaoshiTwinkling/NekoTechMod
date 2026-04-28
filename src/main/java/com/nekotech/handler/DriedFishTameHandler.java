package com.nekotech.handler;

import com.nekotech.item.ModItems;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.random.Random;

public class DriedFishTameHandler {
    public static ActionResult handleTameAttempt(PlayerEntity player, Hand hand, Object entity) {
        if (!(entity instanceof CatEntity cat)) {
            return ActionResult.PASS; // 不是猫，不处理
        }

        if (cat.isTamed()) {
            return ActionResult.PASS; // 已驯服，跳过
        }

        ItemStack stack = player.getStackInHand(hand);
        if (!stack.isOf(ModItems.fish_can)) { // 替换为你的物品实例
            return ActionResult.PASS; // 未持有指定物品，跳过
        }

        if (player.getWorld().isClient) {
            return ActionResult.CONSUME;
        }

        stack.decrement(1); // 减少物品数量

        player.giveItemStack(new ItemStack(ModItems.tin_can));
        // 4. 执行90%概率驯服逻辑
        if (cat.getRandom().nextFloat() < 0.9f) { // 90% 成功
            // 驯服猫
            cat.setOwner(player);

            cat.getWorld().sendEntityStatus(cat, (byte)7);


            return ActionResult.SUCCESS; // 拦截事件，防止原版逻辑执行
        }
        else {
            cat.getWorld().sendEntityStatus(cat, (byte) 6);
        }
        return ActionResult.SUCCESS; // 10% 失败，继续执行原版逻辑（可能无效果）
    }
}

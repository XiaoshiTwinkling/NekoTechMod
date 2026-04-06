package com.nekotech.block.entity.machines;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * 这个接口提供右键放入物品、蹲下右键取出物品的功能喵！
 */
public interface TakeFreelyInventory {


    /**
     * 处理玩家右键交互
     * @param player 玩家
     * @param stack 玩家手中的物品
     * @return 是否处理成功
     */
    default boolean handleRightClick(PlayerEntity player, ItemStack stack) {
        if (player.getWorld().isClient) {
            return true;
        }

        if (player.isSneaking()) {
            // 蹲下右键取出所有物品
            return takeOutAllItems(player);
        } else {
            // 直接右键放入物品
            return putInItem(player, stack);
        }
    }

    /**
     * 放入物品喵
     */
    default boolean putInItem(PlayerEntity player, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        //改为先尝试合并再找空槽位，并修改了尝试合并导致的错误应用

        for (int i = 0; i < getInventorySize(); i++) {
            ItemStack slotStack = getStack(i);

            if (!slotStack.isEmpty()
                    && ItemStack.areItemsAndComponentsEqual(slotStack, stack)
                    && slotStack.getCount() < slotStack.getMaxCount()
                    && canInsert(stack, i)) {

                slotStack.increment(1);

                if (!player.getAbilities().creativeMode) {
                    stack.decrement(1);
                }

                onItemPut(player, slotStack.copy(), i);
                return true;
            }
        }

        for (int i = 0; i < getInventorySize(); i++) {
            if (getStack(i).isEmpty() && canInsert(stack, i)) {
                ItemStack copy = stack.copy();
                copy.setCount(1);
                setStack(i, copy);

                if (!player.getAbilities().creativeMode) {
                    stack.decrement(1);
                }

                onItemPut(player, copy, i);
                return true;
            }
        }

        return false;
    }


    /**
     * 取出所有物品
     */
    default boolean takeOutAllItems(PlayerEntity player) {
        if (isEmpty()) {
            return false;
        }

        boolean tookSomething = false;

        for (int i = 0; i < getInventorySize(); i++) {

            ItemStack extracted = removeStack(i);

            if (!extracted.isEmpty() && canExtract(extracted, i)) {

                if (!player.getInventory().insertStack(extracted)) {
                    player.dropItem(extracted, false);
                }

                tookSomething = true;

                onItemTaken(player, extracted, i);
            }
        }

        if (tookSomething) {
            onAllItemsTaken(player);
        }

        return tookSomething;
    }
    /**
     * 获取物品栏大小喵
     */
    int getInventorySize();

    /**
     * 获取指定槽位的物品喵
     */
    ItemStack getStack(int slot);

    /**
     * 设置指定槽位的物品喵
     */
    void setStack(int slot, ItemStack stack);

    /**
     * 检查物品栏是否为空喵
     */
    default boolean isEmpty() {
        for (int i = 0; i < getInventorySize(); i++) {
            if (!getStack(i).isEmpty()) return false;
        }
        return true;
    }

    /**
     * 标记数据已修改喵
     */
    void markDirty();

    /**
     * 获取方块位置喵
     */
    BlockPos getPos();

    /**
     * 获取世界对象喵
     */
    World getWorld();

    /**
     * 检查是否可以放入物品喵
     */
    default boolean canInsert(ItemStack stack, int slot) {
        return true;
    }

    /**
     * 检查是否可以取出物品喵
     */
    default boolean canExtract(ItemStack stack, int slot) {
        return true;
    }

    /**
     * 物品放入时的回调喵
     */
    default void onItemPut(PlayerEntity player, ItemStack stack, int slot) {
        playInsertSound();
        sendStatusMessage(player, Text.translatable("message.nekotech.item_put", stack.getName()));
    }

    /**
     * 物品取出时的回调喵
     */
    default void onItemTaken(PlayerEntity player, ItemStack stack, int slot) {
        playTakeSound();
    }

    /**
     * 所有物品取出时的回调喵
     */
    default void onAllItemsTaken(PlayerEntity player) {
        sendStatusMessage(player, Text.translatable("message.nekotech.all_items_taken"));
    }


    default void playInsertSound() {
        World world = getWorld();
        if (world != null && !world.isClient()) {
            world.playSound(null, getPos(),
                    SoundEvents.ENTITY_ITEM_PICKUP,
                    SoundCategory.BLOCKS, 0.5f, 1.0f);
        }
    }

    default void playTakeSound() {
        World world = getWorld();
        if (world != null && !world.isClient()) {
            world.playSound(null, getPos(),
                    SoundEvents.ENTITY_ITEM_FRAME_REMOVE_ITEM,
                    SoundCategory.BLOCKS, 0.5f, 1.0f);
        }
    }

    default void sendStatusMessage(PlayerEntity player, Text message) {
        World world = getWorld();
        if (world != null && !world.isClient()) {
            player.sendMessage(message, true);
        }
    }

    /**
     * 获取第一个可用的空槽位喵
     */
    default int getFirstEmptySlot() {
        for (int i = 0; i < getInventorySize(); i++) {
            if (getStack(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 获取第一个非空槽位喵
     */
    default int getFirstNonEmptySlot() {
        for (int i = 0; i < getInventorySize(); i++) {
            if (!getStack(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 获取已使用的槽位数量喵
     */
    default int getUsedSlots() {
        int count = 0;
        for (int i = 0; i < getInventorySize(); i++) {
            if (!getStack(i).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    /**
     * 获取物品总数喵
     */
    default int getTotalItems() {
        int total = 0;
        for (int i = 0; i < getInventorySize(); i++) {
            total += getStack(i).getCount();
        }
        return total;
    }

    /**
     * 清空所有槽位喵
     */
    default void clearAll() {
        for (int i = 0; i < getInventorySize(); i++) {
            setStack(i, ItemStack.EMPTY);
        }
        markDirty();
    }

    /**
     * 检查是否有特定物品喵
     */
    default boolean hasItem(ItemStack target) {
        for (int i = 0; i < getInventorySize(); i++) {
            ItemStack stack = getStack(i);
            if (!stack.isEmpty() && ItemStack.areItemsEqual(target, stack)) {
                return true;
            }
        }
        return false;
    }

    ItemStack removeStack(int slot);

    ItemStack removeStack(int slot, int amount);
}

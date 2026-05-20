package com.nekotech.block.entity.api;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

public interface ImplementedInventory extends Inventory {
    DefaultedList<ItemStack> getItems();

    @Override
    default int size() {
        return getItems().size();
    }

    @Override
    default boolean isEmpty() {
        for (ItemStack stack : getItems()) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    default ItemStack getStack(int slot) {
        return getItems().get(slot);
    }

    @Override
    default ItemStack removeStack(int slot, int amount) {
        ItemStack result = Inventories.splitStack(getItems(), slot, amount);
        if (!result.isEmpty()) {
            markDirty();
        }
        return result;
    }

    @Override
    default ItemStack removeStack(int slot) {
        ItemStack result = Inventories.removeStack(getItems(), slot);
        if (!result.isEmpty()) {
            markDirty();
        }
        return result;
    }

    @Override
    default void setStack(int slot, ItemStack stack) {
        getItems().set(slot, stack);

        if (stack.getCount() > stack.getMaxCount()) {
            stack.setCount(stack.getMaxCount());
        }

        markDirty();
    }


    @Override
    default void clear() {
        for (int i = 0; i < size(); i++) {
            setStack(i, ItemStack.EMPTY);
        }
    }

    @Override
    default boolean canPlayerUse(PlayerEntity player) {
        return true;
    }
}
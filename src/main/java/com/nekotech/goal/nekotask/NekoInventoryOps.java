package com.nekotech.goal.nekotask;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public final class NekoInventoryOps {

    private NekoInventoryOps() {
    }

    public static Inventory getInventoryAt(ServerWorld world, BlockPos pos) {
        BlockEntity blockEntity = world.getBlockEntity(pos);

        if (blockEntity instanceof Inventory inventory) {
            return inventory;
        }

        return null;
    }

    public static ItemStack insertStack(Inventory inventory, ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack remaining = stack.copy();

        // 先尝试合并到已有堆叠。
        for (int slot = 0; slot < inventory.size(); slot++) {
            if (remaining.isEmpty()) {
                break;
            }

            ItemStack current = inventory.getStack(slot);

            if (current.isEmpty()) {
                continue;
            }

            if (!ItemStack.areItemsAndComponentsEqual(current, remaining)) {
                continue;
            }

            if (!inventory.isValid(slot, remaining)) {
                continue;
            }

            int max = Math.min(current.getMaxCount(), inventory.getMaxCountPerStack());
            int space = max - current.getCount();

            if (space <= 0) {
                continue;
            }

            int move = Math.min(space, remaining.getCount());
            current.increment(move);
            remaining.decrement(move);
            inventory.markDirty();
        }

        // 再尝试放进空槽。
        for (int slot = 0; slot < inventory.size(); slot++) {
            if (remaining.isEmpty()) {
                break;
            }

            ItemStack current = inventory.getStack(slot);

            if (!current.isEmpty()) {
                continue;
            }

            if (!inventory.isValid(slot, remaining)) {
                continue;
            }

            int max = Math.min(remaining.getMaxCount(), inventory.getMaxCountPerStack());
            int move = Math.min(max, remaining.getCount());

            ItemStack inserted = remaining.split(move);
            inventory.setStack(slot, inserted);
            inventory.markDirty();
        }

        return remaining;
    }

    public static ItemStack removeOneMatching(Inventory inventory, Item item) {
        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stack = inventory.getStack(slot);

            if (stack.isEmpty()) {
                continue;
            }

            if (!stack.isOf(item)) {
                continue;
            }

            ItemStack taken = inventory.removeStack(slot, 1);
            inventory.markDirty();

            return taken;
        }

        return ItemStack.EMPTY;
    }
}

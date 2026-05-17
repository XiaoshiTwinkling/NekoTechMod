package com.nekotech.goal.nekotask;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;

import java.util.ArrayList;
import java.util.List;

public final class NekoCatTaskData {

    private static final String CARRIED_ITEMS = "neko_carried_items";
    private static final String RECENT_TASKS = "neko_recent_tasks";

    private static final int MEMORY_LIMIT = 5;

    private NekoCatTaskData() {
    }

    public static List<ItemStack> readCarriedStacks(
            NbtCompound data,
            RegistryWrapper.WrapperLookup registries
    ) {
        NbtList list = data.getList(CARRIED_ITEMS, NbtElement.COMPOUND_TYPE);

        List<ItemStack> stacks = new ArrayList<>();

        for (int i = 0; i < list.size(); i++) {
            ItemStack stack = ItemStack.fromNbtOrEmpty(
                    registries,
                    list.getCompound(i)
            );

            if (!stack.isEmpty()) {
                stacks.add(stack);
            }
        }

        return stacks;
    }

    public static void writeCarriedStacks(
            NbtCompound data,
            RegistryWrapper.WrapperLookup registries,
            List<ItemStack> stacks
    ) {
        NbtList list = new NbtList();

        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) {
                continue;
            }

            NbtElement encoded = stack.encode(registries);

            if (encoded instanceof NbtCompound compound) {
                list.add(compound);
            }
        }

        data.put(CARRIED_ITEMS, list);
    }

    public static ItemStack removeOneCarriedItem(
            NbtCompound data,
            RegistryWrapper.WrapperLookup registries,
            Item item
    ) {
        List<ItemStack> stacks = readCarriedStacks(data, registries);

        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);

            if (!stack.isOf(item)) {
                continue;
            }

            ItemStack taken = stack.split(1);

            if (stack.isEmpty()) {
                stacks.remove(i);
            }

            writeCarriedStacks(data, registries, stacks);
            return taken;
        }

        return ItemStack.EMPTY;
    }

    public static void addCarriedStack(
            NbtCompound data,
            RegistryWrapper.WrapperLookup registries,
            ItemStack incoming
    ) {
        if (incoming.isEmpty()) {
            return;
        }

        List<ItemStack> stacks = readCarriedStacks(data, registries);
        ItemStack remaining = incoming.copy();

        for (ItemStack existing : stacks) {
            if (remaining.isEmpty()) {
                break;
            }

            if (!ItemStack.areItemsAndComponentsEqual(existing, remaining)) {
                continue;
            }

            int max = Math.min(existing.getMaxCount(), remaining.getMaxCount());
            int space = max - existing.getCount();

            if (space <= 0) {
                continue;
            }

            int move = Math.min(space, remaining.getCount());
            existing.increment(move);
            remaining.decrement(move);
        }

        if (!remaining.isEmpty()) {
            stacks.add(remaining.copy());
        }

        writeCarriedStacks(data, registries, stacks);
    }

    public static List<NekoTaskMemoryKey> readMemory(NbtCompound data) {
        NbtList list = data.getList(RECENT_TASKS, NbtElement.COMPOUND_TYPE);

        List<NekoTaskMemoryKey> memory = new ArrayList<>();

        for (int i = 0; i < list.size(); i++) {
            memory.add(NekoTaskMemoryKey.fromNbt(list.getCompound(i)));
        }

        return memory;
    }

    public static void remember(
            NbtCompound data,
            NekoTaskMemoryKey key
    ) {
        List<NekoTaskMemoryKey> memory = readMemory(data);

        memory.remove(key);
        memory.add(key);

        while (memory.size() > MEMORY_LIMIT) {
            memory.remove(0);
        }

        writeMemory(data, memory);
    }

    private static void writeMemory(
            NbtCompound data,
            List<NekoTaskMemoryKey> memory
    ) {
        NbtList list = new NbtList();

        for (NekoTaskMemoryKey key : memory) {
            list.add(key.toNbt());
        }

        data.put(RECENT_TASKS, list);
    }
}
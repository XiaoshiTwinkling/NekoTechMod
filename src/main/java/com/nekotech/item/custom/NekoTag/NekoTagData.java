package com.nekotech.item.custom.NekoTag;

import com.nekotech.util.NekoTask;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.Registries;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;

public final class NekoTagData {

    private static final String ROOT = "neko_tag";

    private static final String COLOR = "color";
    private static final String PRIORITY = "priority";
    private static final String TASK = "task";
    private static final String DISPLAY_STACK = "display_stack_id";

    private NekoTagData() {
    }

    private static NbtCompound getAllData(ItemStack stack) {
        NbtComponent component = stack.getOrDefault(
                DataComponentTypes.CUSTOM_DATA,
                NbtComponent.DEFAULT
        );

        return component.copyNbt();
    }

    public static NbtCompound getRoot(ItemStack stack) {
        NbtCompound all = getAllData(stack);

        if (!all.contains(ROOT, NbtElement.COMPOUND_TYPE)) {
            all.put(ROOT, new NbtCompound());
        }

        return all.getCompound(ROOT);
    }

    public static void save(
            ItemStack tagStack,
            DyeColor color,
            short priority,
            NekoTask task,
            ItemStack displayStack
    ) {
        NbtCompound all = getAllData(tagStack);
        NbtCompound root = new NbtCompound();

        root.putString(COLOR, color.getName());
        root.putShort(PRIORITY, priority);
        root.putString(TASK, task.id());

        if (!displayStack.isEmpty()) {
            Identifier itemId = Registries.ITEM.getId(displayStack.getItem());
            root.putString(DISPLAY_STACK, itemId.toString());
        } else {
            root.putString(DISPLAY_STACK, "");
        }

        all.put(ROOT, root);

        tagStack.set(
                DataComponentTypes.CUSTOM_DATA,
                NbtComponent.of(all)
        );
    }

    public static DyeColor readColor(ItemStack stack) {
        String name = getRoot(stack).getString(COLOR);
        return DyeColor.byName(name, DyeColor.WHITE);
    }

    public static short readPriority(ItemStack stack) {
        return getRoot(stack).getShort(PRIORITY);
    }

    public static NekoTask readTask(ItemStack stack) {
        return NekoTask.byId(getRoot(stack).getString(TASK));
    }

    public static ItemStack readDisplayStack(ItemStack stack) {
        String id = getRoot(stack).getString(DISPLAY_STACK);

        if (id == null || id.isEmpty()) {
            return ItemStack.EMPTY;
        }

        Identifier identifier = Identifier.tryParse(id);

        if (identifier == null) {
            return ItemStack.EMPTY;
        }

        Item item = Registries.ITEM.get(identifier);

        if (item == Items.AIR) {
            return ItemStack.EMPTY;
        }

        return new ItemStack(item);
    }
}
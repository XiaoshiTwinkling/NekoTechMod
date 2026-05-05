package com.nekotech.item.custom.NekoTag;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;

public record NekoPlacedTag(
        String color,
        short priority,
        String task,
        String displayStackId
) {
    private static final String COLOR = "color";
    private static final String PRIORITY = "priority";
    private static final String TASK = "task";
    private static final String DISPLAY_STACK = "display_stack_id";

    public static NekoPlacedTag fromStack(ItemStack stack) {
        DyeColor color = NekoTagData.readColor(stack);
        short priority = NekoTagData.readPriority(stack);

        NekoTask task = NekoTagData.readTask(stack);
        String taskId = task == null ? "" : task.id();

        ItemStack displayStack = NekoTagData.readDisplayStack(stack);
        String displayStackId = displayStack.isEmpty()
                ? ""
                : Registries.ITEM.getId(displayStack.getItem()).toString();

        return new NekoPlacedTag(
                color.getName(),
                priority,
                taskId,
                displayStackId
        );
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();

        nbt.putString(COLOR, color);
        nbt.putShort(PRIORITY, priority);
        nbt.putString(TASK, task);
        nbt.putString(DISPLAY_STACK, displayStackId);

        return nbt;
    }

    public static NekoPlacedTag fromNbt(NbtCompound nbt) {
        String color = nbt.getString(COLOR);
        short priority = nbt.getShort(PRIORITY);
        String task = nbt.getString(TASK);
        String displayStackId = nbt.getString(DISPLAY_STACK);

        return new NekoPlacedTag(
                color == null ? "" : color,
                priority,
                task == null ? "" : task,
                displayStackId == null ? "" : displayStackId
        );
    }

    public DyeColor dyeColor() {
        return DyeColor.byName(color, DyeColor.WHITE);
    }

    public Identifier displayIdentifierOrNull() {
        if (displayStackId == null || displayStackId.isEmpty()) {
            return null;
        }

        return Identifier.tryParse(displayStackId);
    }
}
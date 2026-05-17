package com.nekotech.goal.nekotask;

import com.nekotech.data.worlddata.NekoTagWorldState;
import com.nekotech.item.custom.NekoTag.NekoPlacedTag;
import net.minecraft.nbt.NbtCompound;

public record NekoTaskMemoryKey(
        String dimension,
        long posLong,
        String color,
        short priority,
        String task,
        String displayStackId
) {
    private static final String DIMENSION = "dimension";
    private static final String POS = "pos";
    private static final String COLOR = "color";
    private static final String PRIORITY = "priority";
    private static final String TASK = "task";
    private static final String DISPLAY_STACK = "display_stack_id";

    public static NekoTaskMemoryKey of(NekoTagWorldState.TaskCandidate candidate) {
        NekoPlacedTag tag = candidate.tag();

        return new NekoTaskMemoryKey(
                candidate.location().dimension(),
                candidate.location().posLong(),
                tag.color(),
                tag.priority(),
                tag.task(),
                tag.displayStackId()
        );
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();

        nbt.putString(DIMENSION, dimension);
        nbt.putLong(POS, posLong);
        nbt.putString(COLOR, color);
        nbt.putShort(PRIORITY, priority);
        nbt.putString(TASK, task);
        nbt.putString(DISPLAY_STACK, displayStackId);

        return nbt;
    }

    public static NekoTaskMemoryKey fromNbt(NbtCompound nbt) {
        return new NekoTaskMemoryKey(
                nbt.getString(DIMENSION),
                nbt.getLong(POS),
                nbt.getString(COLOR),
                nbt.getShort(PRIORITY),
                nbt.getString(TASK),
                nbt.getString(DISPLAY_STACK)
        );
    }
}
package com.nekotech.item.custom.NekoTag;

import net.minecraft.util.Identifier;

public enum NekoTask {
    INPUT("input", "gui/task/input.png"),
    OUTPUT("output", "gui/task/output.png");

    public static final String MOD_ID = "neko-technology";

    private final String id;
    private final Identifier texture;

    NekoTask(String id, String texturePath) {
        this.id = id;
        this.texture = Identifier.of(MOD_ID, "textures/" + texturePath);
    }

    public String id() {
        return id;
    }

    public Identifier texture() {
        return texture;
    }

    public static NekoTask byId(String id) {
        for (NekoTask task : values()) {
            if (task.id.equals(id)) {
                return task;
            }
        }

        return INPUT;
    }

    public String translationKey() {
        return "task.neko-technology." + this.id();
    }
}
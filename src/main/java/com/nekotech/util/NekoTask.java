package com.nekotech.util;

import net.minecraft.util.Identifier;

public enum NekoTask {
    IDLE("idle", "gui/task/idle.png");
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

        return IDLE;
    }

    public String translationKey() {
        return "task.neko-technology." + this.id();
    }
}
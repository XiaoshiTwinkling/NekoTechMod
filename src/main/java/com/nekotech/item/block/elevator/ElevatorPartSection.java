package com.nekotech.item.block.elevator;

import net.minecraft.util.StringIdentifiable;

public enum ElevatorPartSection implements StringIdentifiable {
    MIDDLE("middle"),
    TOP("top");

    private final String name;

    ElevatorPartSection(String name) {
        this.name = name;
    }

    @Override
    public String asString() {
        return name;
    }
}

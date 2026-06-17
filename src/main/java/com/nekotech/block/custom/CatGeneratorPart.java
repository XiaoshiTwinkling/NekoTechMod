package com.nekotech.block.custom;

import net.minecraft.util.StringIdentifiable;

public enum CatGeneratorPart implements StringIdentifiable {
    LEFT("left"),
    RIGHT("right");

    private final String name;

    CatGeneratorPart(String name) {
        this.name = name;
    }

    @Override
    public String asString() {
        return this.name;
    }
}

package com.nekotech.recipe.AlloyPot;

import net.minecraft.item.ItemStack;
import net.minecraft.recipe.input.RecipeInput;

public record AlloyPotRecipeInput(ItemStack input1, ItemStack input2) implements RecipeInput {
    @Override
    public ItemStack getStackInSlot(int slot) {
        return switch (slot) {
            case 0 -> input1;
            case 1 -> input2;
            default -> throw new IllegalArgumentException("Invalid slot: " + slot);
        };
    }

    @Override
    public int getSize() {
        return 2;
    }
}
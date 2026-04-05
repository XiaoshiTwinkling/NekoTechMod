package com.nekotech.recipe.AlloyPot;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.MapCodec;
import com.nekotech.recipe.ModRecipes;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;

public record AlloyRecipe(
        Ingredient input1,
        Ingredient input2,
        ItemStack result1,
        ItemStack result2,
        int minTemperature,
        int cookTime
) implements Recipe<AlloyPotRecipeInput> {

    public static final MapCodec<AlloyRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.DISALLOW_EMPTY_CODEC.fieldOf("input1").forGetter(AlloyRecipe::input1),
            Ingredient.DISALLOW_EMPTY_CODEC.fieldOf("input2").forGetter(AlloyRecipe::input2),
            ItemStack.VALIDATED_CODEC.fieldOf("result1").forGetter(AlloyRecipe::result1),
            ItemStack.VALIDATED_CODEC.fieldOf("result2").forGetter(AlloyRecipe::result2),
            com.mojang.serialization.Codec.INT.fieldOf("min_temperature").forGetter(AlloyRecipe::minTemperature),
            com.mojang.serialization.Codec.INT.fieldOf("cook_time").forGetter(AlloyRecipe::cookTime)
    ).apply(instance, AlloyRecipe::new));

    public static final PacketCodec<RegistryByteBuf, AlloyRecipe> PACKET_CODEC = PacketCodec.tuple(
            Ingredient.PACKET_CODEC, AlloyRecipe::input1,
            Ingredient.PACKET_CODEC, AlloyRecipe::input2,
            ItemStack.PACKET_CODEC, AlloyRecipe::result1,
            ItemStack.PACKET_CODEC, AlloyRecipe::result2,
            PacketCodecs.INTEGER, AlloyRecipe::minTemperature,
            PacketCodecs.INTEGER, AlloyRecipe::cookTime,
            AlloyRecipe::new
    );

    @Override
    public boolean matches(AlloyPotRecipeInput input, World world) {
        return (
                input1.test(input.input1()) && input2.test(input.input2())
        ) || (
                input1.test(input.input2()) && input2.test(input.input1())
        );
    }

    @Override
    public ItemStack craft(AlloyPotRecipeInput input, RegistryWrapper.WrapperLookup lookup) {
        return result1.copy();
    }

    @Override
    public ItemStack getResult(RegistryWrapper.WrapperLookup lookup) {
        return result1.copy();
    }

    public ItemStack getSecondResult() {
        return result2.copy();
    }

    @Override
    public boolean fits(int width, int height) {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.ALLOY_SERIALIZER;
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.ALLOY_TYPE;
    }
}

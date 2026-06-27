package com.nekotech.recipe.WorkBench.forging;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.nekotech.recipe.ModRecipes;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.input.SingleStackRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public class ForgingRecipe implements Recipe<SingleStackRecipeInput> {
    private final Ingredient input;
    private final ItemStack output;
    private final float successChance; // 成功概率
    private Identifier id;

    public static final MapCodec<ForgingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Ingredient.DISALLOW_EMPTY_CODEC.fieldOf("input").forGetter(ForgingRecipe::getInput),
                    ItemStack.VALIDATED_CODEC.fieldOf("output").forGetter(recipe -> recipe.getResult(null)),
                    Codec.FLOAT.optionalFieldOf("success_chance", 0.5f).forGetter(ForgingRecipe::getSuccessChance)
            ).apply(instance, ForgingRecipe::new)
    );

    public static final PacketCodec<RegistryByteBuf, ForgingRecipe> PACKET_CODEC = PacketCodec.tuple(
            Ingredient.PACKET_CODEC,
            ForgingRecipe::getInput,
            ItemStack.PACKET_CODEC,
            recipe -> recipe.getResult(null),
            PacketCodecs.FLOAT,
            ForgingRecipe::getSuccessChance,
            ForgingRecipe::new
    );

    public ForgingRecipe(Ingredient input, ItemStack output, float successChance) {
        this.input = input;
        this.output = output;
        this.successChance = successChance;
    }

    public void setId(Identifier id) {
        this.id = id;
    }

    @Override
    public boolean matches(SingleStackRecipeInput input, World world) {
        return this.input.test(input.getStackInSlot(0));
    }

    @Override
    public ItemStack craft(SingleStackRecipeInput input, RegistryWrapper.WrapperLookup lookup) {
        return this.output.copy();
    }

    @Override
    public boolean fits(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResult(RegistryWrapper.WrapperLookup registriesLookup) {
        return this.output;
    }

    public Identifier getId() {
        return this.id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.FORGING_RECIPE_SERIALIZER;
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.FORGING_RECIPE_TYPE;
    }

    public Ingredient getInput() {
        return input;
    }

    public ItemStack getOutput() { return output; }

    public float getSuccessChance() {
        return successChance;
    }
}

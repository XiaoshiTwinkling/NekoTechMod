package com.nekotech.recipe.ChargeStation;

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

public class ChargeRecipe implements Recipe<SingleStackRecipeInput> {
    private final Ingredient input;
    private final ItemStack output;
    private final int chargeTime; // 充能所需时间（tick）
    private Identifier id;

    public static final MapCodec<ChargeRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Ingredient.DISALLOW_EMPTY_CODEC.fieldOf("input").forGetter(ChargeRecipe::getInput),
                    ItemStack.VALIDATED_CODEC.fieldOf("output").forGetter(recipe -> recipe.getResult(null)),
                    Codec.INT.optionalFieldOf("charge_time", 100).forGetter(ChargeRecipe::getChargeTime)
            ).apply(instance, ChargeRecipe::new)
    );

    public static final PacketCodec<RegistryByteBuf, ChargeRecipe> PACKET_CODEC = PacketCodec.tuple(
            Ingredient.PACKET_CODEC,
            ChargeRecipe::getInput,
            ItemStack.PACKET_CODEC,
            recipe -> recipe.getResult(null),
            PacketCodecs.INTEGER,
            ChargeRecipe::getChargeTime,
            ChargeRecipe::new
    );

    public ChargeRecipe(Ingredient input, ItemStack output, int chargeTime) {
        this.input = input;
        this.output = output;
        this.chargeTime = chargeTime;
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
        return ModRecipes.CHARGE_RECIPE_SERIALIZER;
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.CHARGE_RECIPE_TYPE;
    }

    public Ingredient getInput() {
        return input;
    }

    public ItemStack getOutput() {
        return output;
    }

    public int getChargeTime() {
        return chargeTime;
    }
}

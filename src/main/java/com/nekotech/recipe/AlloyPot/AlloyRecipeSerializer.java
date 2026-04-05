package com.nekotech.recipe.AlloyPot;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;


import net.minecraft.recipe.RecipeSerializer;

public class AlloyRecipeSerializer implements RecipeSerializer<AlloyRecipe> {
    @Override
    public MapCodec<AlloyRecipe> codec() {
        return AlloyRecipe.CODEC;
    }

    @Override
    public PacketCodec<RegistryByteBuf, AlloyRecipe> packetCodec() {
        return AlloyRecipe.PACKET_CODEC;
    }
}
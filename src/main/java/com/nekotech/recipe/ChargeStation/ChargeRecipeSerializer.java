package com.nekotech.recipe.ChargeStation;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.recipe.RecipeSerializer;

public class ChargeRecipeSerializer implements RecipeSerializer<ChargeRecipe> {
    @Override
    public MapCodec<ChargeRecipe> codec() {
        return ChargeRecipe.CODEC;
    }

    @Override
    public PacketCodec<RegistryByteBuf, ChargeRecipe> packetCodec() {
        return ChargeRecipe.PACKET_CODEC;
    }
}

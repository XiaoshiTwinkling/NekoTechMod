package com.nekotech.recipe.WorkBench.forging;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.recipe.RecipeSerializer;

public class ForgingRecipeSerializer implements RecipeSerializer<ForgingRecipe> {
    @Override
    public MapCodec<ForgingRecipe> codec() {
        return ForgingRecipe.CODEC; // 直接返回配方类中定义的 CODEC
    }

    @Override
    public PacketCodec<RegistryByteBuf, ForgingRecipe> packetCodec() {
        return ForgingRecipe.PACKET_CODEC; // 直接返回配方类中定义的 PACKET_CODEC
    }
}

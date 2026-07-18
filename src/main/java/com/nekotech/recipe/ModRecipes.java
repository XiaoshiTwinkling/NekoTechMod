package com.nekotech.recipe;

import com.nekotech.NekoTechnology;
import com.nekotech.recipe.AlloyPot.AlloyRecipe;
import com.nekotech.recipe.AlloyPot.AlloyRecipeSerializer;
import com.nekotech.recipe.ChargeStation.ChargeRecipe;
import com.nekotech.recipe.ChargeStation.ChargeRecipeSerializer;
import com.nekotech.recipe.WorkBench.forging.ForgingRecipe;
import com.nekotech.recipe.WorkBench.forging.ForgingRecipeSerializer;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModRecipes {

    public static RecipeType<AlloyRecipe> ALLOY_TYPE;
    public static RecipeSerializer<AlloyRecipe> ALLOY_SERIALIZER;

    public static RecipeType<ForgingRecipe> FORGING_RECIPE_TYPE;
    public static RecipeSerializer<ForgingRecipe> FORGING_RECIPE_SERIALIZER;

    public static RecipeType<ChargeRecipe> CHARGE_RECIPE_TYPE;
    public static RecipeSerializer<ChargeRecipe> CHARGE_RECIPE_SERIALIZER;

    public static void init() {
        ALLOY_TYPE = Registry.register(
                Registries.RECIPE_TYPE,
                Identifier.of(NekoTechnology.MOD_ID, "alloy"),
                new RecipeType<AlloyRecipe>() {
                    @Override
                    public String toString() {
                        return "neko-technology:alloy";
                    }
                });

        ALLOY_SERIALIZER = Registry.register(
                Registries.RECIPE_SERIALIZER,
                Identifier.of(NekoTechnology.MOD_ID, "alloy"),
                new AlloyRecipeSerializer()
        );

        FORGING_RECIPE_TYPE = Registry.register(
                Registries.RECIPE_TYPE,
                Identifier.of(NekoTechnology.MOD_ID, "forging"),
                new RecipeType<ForgingRecipe>() {
                    @Override
                    public String toString() {
                        return "neko-technology:forging";
                    }
                });

        FORGING_RECIPE_SERIALIZER = Registry.register(
                Registries.RECIPE_SERIALIZER,
                Identifier.of(NekoTechnology.MOD_ID, "forging"),
                new ForgingRecipeSerializer()
        );

        CHARGE_RECIPE_TYPE = Registry.register(
                Registries.RECIPE_TYPE,
                Identifier.of(NekoTechnology.MOD_ID, "charge"),
                new RecipeType<ChargeRecipe>() {
                    @Override
                    public String toString() {
                        return "neko-technology:charge";
                    }
                });

        CHARGE_RECIPE_SERIALIZER = Registry.register(
                Registries.RECIPE_SERIALIZER,
                Identifier.of(NekoTechnology.MOD_ID, "charge"),
                new ChargeRecipeSerializer()
        );

    }
}
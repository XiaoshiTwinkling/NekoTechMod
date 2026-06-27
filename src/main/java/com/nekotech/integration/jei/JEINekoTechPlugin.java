package com.nekotech.integration.jei;

import com.nekotech.recipe.WorkBench.forging.ForgingRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.recipe.Ingredient;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;

import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public class JEINekoTechPlugin implements IModPlugin {
    @Override
    public Identifier getPluginUid() {
        return null;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        // 注册自定义配方类别
        registration.addRecipeCategories(new WorkBenchRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // 注册具体的配方实例
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        // 注册GUI交互器
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        // 注册配方催化剂
    }
}

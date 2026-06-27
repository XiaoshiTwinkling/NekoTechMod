package com.nekotech.integration.jei;

import com.nekotech.NekoTechnology;
import com.nekotech.block.ModBlocks;
import com.nekotech.recipe.WorkBench.forging.ForgingRecipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

public class WorkBenchRecipeCategory implements IRecipeCategory<ForgingRecipe> {
    public static final RecipeType<ForgingRecipe> TYPE =
            RecipeType.create(NekoTechnology.MOD_ID, "work_bench", ForgingRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;

    public WorkBenchRecipeCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(116, 56);
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
                new ItemStack(ModBlocks.WORK_BENCH));
    }

    @Override
    public @NotNull RecipeType<ForgingRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public @NotNull Text getTitle() {
        return Text.translatable("block.neko-technology.work_bench");
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ForgingRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 18, 20)
                .addIngredients(recipe.getInput());

        builder.addSlot(RecipeIngredientRole.OUTPUT, 96, 20)
                .addItemStack(recipe.getOutput());
    }

}

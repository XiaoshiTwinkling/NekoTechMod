package com.nekotech.block.entity.machines;

import com.nekotech.block.entity.ModBlockEntities;
import com.nekotech.item.block.ModBlocks;
import com.nekotech.recipe.AlloyPot.AlloyPotRecipeInput;
import com.nekotech.recipe.AlloyPot.AlloyRecipe;
import com.nekotech.recipe.ModRecipes;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Optional;

public class AlloyPotBlockEntity extends TakeFreelyMachineBlockEntity{

    public AlloyPotBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.alloy_pot, pos, state, 4);
    }

    private RecipeEntry<AlloyRecipe> cachedRecipe = null;

    public static final int INPUT_SLOT_1 = 0;     // 输入槽1
    public static final int INPUT_SLOT_2 = 1;     // 输入槽2

    public static final int OUTPUT_SLOT_1 = 2;
    public static final int OUTPUT_SLOT_2 = 3;


    private float temperature = 0;  // 当前温度

    private int alloyProgress = 0;      // 当前合金进度
    private int alloyTimeTotal = 0;     // 总所需时间

    public static void tick(World world, BlockPos pos, BlockState state, AlloyPotBlockEntity blockEntity) {
        if (world.isClient) {
            blockEntity.clientTick();
        } else {
            blockEntity.serverTick();
        }
    }

    @Override
    public boolean canInsert(ItemStack stack, int slot) {
        // 输出槽禁止放入
        if (slot == OUTPUT_SLOT_1 || slot == OUTPUT_SLOT_2) {
            return false;
        }

        // 输入槽允许
        return slot == INPUT_SLOT_1 || slot == INPUT_SLOT_2;
    }

    @Override
    public boolean canExtract(ItemStack stack, int slot) {
        // 输入槽不允许直接取（防止破坏配方）
        if (slot == INPUT_SLOT_1 || slot == INPUT_SLOT_2) {
            return false;
        }

        // 输出槽允许取
        return slot == OUTPUT_SLOT_1 || slot == OUTPUT_SLOT_2;
    }

    private Optional<RecipeEntry<AlloyRecipe>> getCurrentRecipe() {
        if (world == null) return Optional.empty();

        AlloyPotRecipeInput input = new AlloyPotRecipeInput(
                getStack(INPUT_SLOT_1),
                getStack(INPUT_SLOT_2)
        );

        return world.getRecipeManager().getFirstMatch(ModRecipes.ALLOY_TYPE, input, world);
    }

    private void insertOutput(int slot, ItemStack stack) {
        ItemStack existing = getStack(slot);

        if (existing.isEmpty()) {
            setStack(slot, stack);
        } else {
            existing.increment(stack.getCount());
        }
    }

    public void clientTick() {
        // 客户端逻辑
    }

    private void serverTick() {
        updateTemperature();

        if (isInputEmpty()) {
            cachedRecipe = null;
            resetProgress();
            return;
        }

        if (cachedRecipe == null) {
            cachedRecipe = getCurrentRecipe().orElse(null);
            resetProgress();
        }

        if (cachedRecipe == null) return;

        AlloyRecipe recipe = cachedRecipe.value();

        if (!canWork(recipe)) {
            return;
        }

        alloyTimeTotal = recipe.cookTime();
        alloyProgress++;

        if (alloyProgress >= alloyTimeTotal) {
            craft(recipe);
            cachedRecipe = null;
            resetProgress();
        }


        markDirty();
    }

    private boolean canWork(AlloyRecipe recipe) {
        if (!canMachineRun()) return false;

        if (!isHeater(getWorld().getBlockState(getPos().down()))) return false;

        if (temperature < recipe.minTemperature()) {
            resetProgress();
            return false;
        }

        return canOutput(recipe);
    }

    private boolean canOutput(AlloyRecipe recipe) {
        var out1 = getStack(OUTPUT_SLOT_1);
        var out2 = getStack(OUTPUT_SLOT_2);

        var r1 = recipe.result1();
        var r2 = recipe.getSecondResult();

        if (!out1.isEmpty() && (!out1.isOf(r1.getItem()) || out1.getCount() + r1.getCount() > out1.getMaxCount()))
            return false;

        if (!out2.isEmpty() && (!out2.isOf(r2.getItem()) || out2.getCount() + r2.getCount() > out2.getMaxCount()))
            return false;

        return true;
    }


    private void updateTemperature() {
        float target = 0f;

        if (getWorld() != null) {
            BlockState belowState = getWorld().getBlockState(getPos().down());

            if (belowState != null && isHeater(belowState)) {
                target = getHeaterTemperature();
            }
        }

        float rate = 0.05f; // 调整升温/降温速度

        temperature += (target - temperature) * rate;

        // 防止无限接近
        if (Math.abs(target - temperature) < 0.01f) {
            temperature = target;
        }
    }

    private void craft(AlloyRecipe recipe) {

        removeStack(INPUT_SLOT_1, 1);
        removeStack(INPUT_SLOT_2, 1);

        insertOutput(OUTPUT_SLOT_1, recipe.result1().copy());
        insertOutput(OUTPUT_SLOT_2, recipe.getSecondResult());
    }

    private boolean isHeater(BlockState belowState) {
        return belowState.isOf(ModBlocks.heater);
    }

    public HeaterBlockEntity getHeaterBelow() {
        if (this.world == null) return null;

        BlockPos belowPos = this.pos.down();
        BlockEntity blockEntity = this.world.getBlockEntity(belowPos);

        if (blockEntity instanceof HeaterBlockEntity heater) {
            return heater;
        }
        return null;
    }

    public float getHeaterTemperature() {
        HeaterBlockEntity heater = getHeaterBelow();
        return heater != null ? heater.getTemperature() : 0f;
    }

    public float getHeaterMaxTemperature() {
        HeaterBlockEntity heater = getHeaterBelow();
        return heater != null ? heater.getMax_temperature() : 0f;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        super.setStack(slot, stack);

        if (slot == INPUT_SLOT_1 || slot == INPUT_SLOT_2) {
            cachedRecipe = null;
            resetProgress();
        }
    }

    private void resetProgress() {
        alloyProgress = 0;
    }

    private boolean isInputEmpty() {
        return getStack(INPUT_SLOT_1).isEmpty()
                && getStack(INPUT_SLOT_2).isEmpty();
    }


    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack result = super.removeStack(slot, amount);

        if (slot == INPUT_SLOT_1 || slot == INPUT_SLOT_2) {
            cachedRecipe = null;
            resetProgress();
        }

        return result;
    }
}
package com.nekotech.block.entity.machines;

import com.nekotech.block.entity.ModBlockEntities;
import com.nekotech.block.entity.api.TakeFreelyInventory;
import com.nekotech.block.custom.WorkBench;
import com.nekotech.recipe.ModRecipes;
import com.nekotech.recipe.WorkBench.forging.ForgingRecipe;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.input.SingleStackRecipeInput;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * 加工台 可以放东西在上面喵
 */
public class WorkBenchBlockEntity extends TakeFreelyMachineBlockEntity implements TakeFreelyInventory , SidedInventory {
    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    public static final int INVENTORY_SIZE = 2;

    public WorkBenchBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.WORK_BENCH, pos, state);
    }

    protected WorkBenchBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state
    ) {
        super(type, pos, state, INVENTORY_SIZE);
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return true;
    }

    @Override
    public boolean canInsert(ItemStack stack, int slot) {
        return slot == INPUT_SLOT;
    }


    @Override
    public int[] getAvailableSlots(Direction side) {
        int[] inputSlots = {INPUT_SLOT};
        int[] outputSlots = {OUTPUT_SLOT};

        if (side == Direction.UP) return inputSlots;
        if (side == Direction.DOWN) return outputSlots;
        return new int[]{INPUT_SLOT, OUTPUT_SLOT};
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return canInsert(stack, slot);
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return canExtract(stack, slot);
    }

    /**
     * 尝试执行锻造配方
     * @param player 执行锻造的玩家
     * @param hammerStack 玩家手中的锤子
     * @return 是否成功执行了锻造
     */
    public boolean tryForging(PlayerEntity player, ItemStack hammerStack) {
        PreparedForging prepared = prepareForging(player);
        if (prepared == null) return false;

        if (hammerStack.getDamage() >= hammerStack.getMaxDamage() - 1) {
            if (player instanceof ServerPlayerEntity) {
                player.sendMessage(Text.translatable("message.neko-technology.forging.hammer_broken"), true);
            }
            return false;
        }

        Random random = world.random;
        float successChance = prepared.recipe().getSuccessChance();

        if (random.nextFloat() <= successChance) {
            return executeForging(player, hammerStack, prepared, true);
        } else {
            return executeForging(player, hammerStack, prepared, false);
        }
    }

    /**
     * 尝试执行一次无需玩家和锤子的锻造。
     *
     * @return 是否实际开始了一次锻造；配方概率失败也视为已经执行
     */
    protected boolean tryAutomatedForging() {
        PreparedForging prepared = prepareForging(null);
        if (prepared == null || world == null) return false;

        boolean success = world.random.nextFloat() <= prepared.recipe().getSuccessChance();
        executeForging(null, null, prepared, success);

        if (success) {
            world.playSound(null, pos, SoundEvents.BLOCK_ANVIL_USE,
                    SoundCategory.BLOCKS, 0.8f, 0.8f + world.random.nextFloat() * 0.4f);
        } else {
            world.playSound(null, pos, SoundEvents.BLOCK_ANVIL_LAND,
                    SoundCategory.BLOCKS, 0.5f, 0.5f);
        }

        return true;
    }

    @Nullable
    private PreparedForging prepareForging(@Nullable PlayerEntity player) {
        if (world == null || world.isClient) {
            return null;
        }

        ItemStack inputStack = getStack(INPUT_SLOT);
        if (inputStack.isEmpty()) {
            sendForgingMessage(player, "message.neko-technology.forging.no_input");
            return null;
        }

        ForgingRecipe recipe = findMatchingRecipe(inputStack);
        if (recipe == null) {
            sendForgingMessage(player, "message.neko-technology.forging.no_recipe");
            return null;
        }

        ItemStack outputStack = getStack(OUTPUT_SLOT);
        ItemStack recipeOutput = recipe.getResult(world.getRegistryManager()).copy();

        if (!outputStack.isEmpty()) {
            boolean canCombine = ItemStack.areItemsAndComponentsEqual(outputStack, recipeOutput);
            boolean isFull = outputStack.getCount() + recipeOutput.getCount() > outputStack.getMaxCount();
            if (!canCombine || isFull) {
                sendForgingMessage(player, "message.neko-technology.forging.output_full");
                return null;
            }
        }

        return new PreparedForging(inputStack, outputStack, recipeOutput, recipe);
    }

    private void sendForgingMessage(@Nullable PlayerEntity player, String translationKey) {
        if (player instanceof ServerPlayerEntity) {
            player.sendMessage(Text.translatable(translationKey), true);
        }
    }

    /**
     * 查找匹配的锻造配方
     */
    @Nullable
    private ForgingRecipe findMatchingRecipe(ItemStack inputStack) {
        if (world == null) {
            return null;
        }

        SingleStackRecipeInput input = new SingleStackRecipeInput(inputStack);
        Optional<RecipeEntry<ForgingRecipe>> recipe = world.getRecipeManager()
                .getFirstMatch(ModRecipes.FORGING_RECIPE_TYPE, input, world);

        return recipe.map(RecipeEntry::value).orElse(null);
    }

    /**
     * 执行锻造操作
     * @param success 是否成功
     */
    private boolean executeForging(@Nullable PlayerEntity player,
                                   @Nullable ItemStack hammerStack,
                                   PreparedForging prepared,
                                   boolean success) {
        if (world == null) {
            return false;
        }

        ItemStack inputStack = prepared.inputStack();
        ItemStack outputStack = prepared.outputStack();
        ItemStack recipeOutput = prepared.recipeOutput();

        inputStack.decrement(1);
        if (inputStack.isEmpty()) {
            setStack(INPUT_SLOT, ItemStack.EMPTY);
        } else {
            setStack(INPUT_SLOT, inputStack);
        }

        if (player != null && hammerStack != null && !player.getAbilities().creativeMode) {
            hammerStack.damage(1, player, EquipmentSlot.MAINHAND);

            // 检查锤子是否损坏
            if (hammerStack.getDamage() >= hammerStack.getMaxDamage()) {
                world.playSound(null, pos, SoundEvents.ENTITY_ITEM_BREAK, SoundCategory.BLOCKS, 0.8f, 0.8f);

                if (player instanceof ServerPlayerEntity) {
                    player.setStackInHand(player.getActiveHand(), ItemStack.EMPTY);
                }
            }
        }

        if (success) {
            if (outputStack.isEmpty()) {
                setStack(OUTPUT_SLOT, recipeOutput);
            } else {
                outputStack.increment(recipeOutput.getCount());
                setStack(OUTPUT_SLOT, outputStack);
            }

            if (world instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(ParticleTypes.CRIT,
                        pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                        10, 0.3, 0.1, 0.3, 0.05);
            }

            if (player instanceof ServerPlayerEntity) {
                player.sendMessage(Text.translatable("message.neko-technology.forging.success")
                        .append(recipeOutput.getName()), true);
            }
        } else {
            if (world instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(ParticleTypes.SMOKE,
                        pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                        5, 0.2, 0.1, 0.2, 0.02);
            }

            if (player instanceof ServerPlayerEntity) {
                player.sendMessage(Text.translatable("message.neko-technology.forging.failed"), true);
            }
        }
        markDirty();

        return success;
    }

    private record PreparedForging(
            ItemStack inputStack,
            ItemStack outputStack,
            ItemStack recipeOutput,
            ForgingRecipe recipe
    ) {
    }

    /**
     * 检查是否有玻璃罩
     */
    public boolean hasGlassCover() {
        if (world == null || pos == null) return false;
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof WorkBench) {
            return state.get(WorkBench.HAS_GLASS_COVER);
        }
        return false;
    }

    /**
     * 在有玻璃罩时禁用交互
     */
    @Override
    public boolean handleRightClick(PlayerEntity player, ItemStack stack) {
        if (hasGlassCover()) {
            return false; // 有玻璃罩时禁止交互
        }
        return super.handleRightClick(player, stack);
    }
}

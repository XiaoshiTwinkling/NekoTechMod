package com.nekotech.block.entity.machines;

import java.util.ArrayList;
import com.nekotech.block.entity.ModBlockEntities;
import com.nekotech.block.entity.api.TakeFreelyInventory;
import com.nekotech.recipe.ModRecipes;
import com.nekotech.recipe.WorkBench.forging.ForgingRecipe;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.input.SingleStackRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * 加工台 可以放东西在上面喵
 */
public class WorkBenchBlockEntity extends BlockEntity implements TakeFreelyInventory , SidedInventory {
    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    public static final int INVENTORY_SIZE = 2;

    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);

    public WorkBenchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.work_bench, pos, state);
    }

    @Override
    public int getInventorySize() {
        return INVENTORY_SIZE;
    }

    @Override
    public ItemStack getStack(int slot) {
        if (slot < 0 || slot >= inventory.size()) {
            return ItemStack.EMPTY;
        }
        return inventory.get(slot);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        if (slot >= 0 && slot < inventory.size()) {

            inventory.set(slot, stack);
            markDirty();

            if (world != null && !world.isClient) {
                world.updateListeners(pos, getCachedState(), getCachedState(),
                        Block.NOTIFY_ALL | Block.REDRAW_ON_MAIN_THREAD);
            }
        }
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return true;
    }

    @Override
    public ItemStack removeStack(int slot) {
        ItemStack result = getStack(slot).copy();
        setStack(slot, ItemStack.EMPTY);
        return result;
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack stackInSlot = getStack(slot);
        if (stackInSlot.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack result = stackInSlot.split(amount);
        if (stackInSlot.isEmpty()) {
            setStack(slot, ItemStack.EMPTY);
        } else {
            setStack(slot, stackInSlot);
        }
        return result;
    }

    @Override
    public boolean canInsert(ItemStack stack, int slot) {
        return slot == INPUT_SLOT;
    }


    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        Inventories.writeNbt(nbt, inventory, true, registryLookup);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {

        Inventories.readNbt(nbt, this.inventory, registryLookup);

        for (int i = 0; i < this.inventory.size(); i++) {
            ItemStack stack = this.inventory.get(i);
            if (stack.getCount() <= 0) {
                this.inventory.set(i, ItemStack.EMPTY);
            }
        }
    }

    @Nullable
    @Override
    public BlockEntityUpdateS2CPacket toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registryLookup) {
        NbtCompound nbt = super.toInitialChunkDataNbt(registryLookup);
        this.writeNbt(nbt, registryLookup);
        return nbt;
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

    @Override
    public int size() { return getInventorySize(); }

    @Override
    public boolean isEmpty() { return TakeFreelyInventory.super.isEmpty(); }

    @Override
    public void clear() { clearAll(); }

    /**
     * 尝试执行锻造配方
     * @param player 执行锻造的玩家
     * @param hammerStack 玩家手中的锤子
     * @return 是否成功执行了锻造
     */
    public boolean tryForging(PlayerEntity player, ItemStack hammerStack) {
        if (world == null || world.isClient) {
            return false;
        }

        ItemStack inputStack = getStack(INPUT_SLOT);
        if (inputStack.isEmpty()) {
            if (player instanceof ServerPlayerEntity) {
                player.sendMessage(Text.translatable("message.neko-technology.forging.no_input"), true);
            }
            return false;
        }

        ForgingRecipe recipe = findMatchingRecipe(inputStack);
        if (recipe == null) {
            if (player instanceof ServerPlayerEntity) {
                player.sendMessage(Text.translatable("message.neko-technology.forging.no_recipe"), true);
            }
            return false;
        }

        ItemStack outputStack = getStack(OUTPUT_SLOT);
        ItemStack recipeOutput = recipe.getResult(world.getRegistryManager()).copy();

        if (!outputStack.isEmpty()) {
            boolean canCombine = ItemStack.areItemsAndComponentsEqual(outputStack, recipeOutput);
            boolean isFull = outputStack.getCount() + recipeOutput.getCount() > outputStack.getMaxCount();
            if (!canCombine || isFull) {
                if (player instanceof ServerPlayerEntity) {
                    player.sendMessage(Text.translatable("message.neko-technology.forging.output_full"), true);
                }
                return false;
            }
        }

        if (hammerStack.getDamage() >= hammerStack.getMaxDamage() - 1) {
            if (player instanceof ServerPlayerEntity) {
                player.sendMessage(Text.translatable("message.neko-technology.forging.hammer_broken"), true);
            }
            return false;
        }

        Random random = world.random;
        float successChance = recipe.getSuccessChance();

        if (random.nextFloat() <= successChance) {

            return executeForging(player, hammerStack, inputStack, outputStack, recipeOutput, recipe, true);
        } else {

            return executeForging(player, hammerStack, inputStack, outputStack, recipeOutput, recipe, false);
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
    private boolean executeForging(PlayerEntity player, ItemStack hammerStack,
                                   ItemStack inputStack, ItemStack outputStack,
                                   ItemStack recipeOutput, ForgingRecipe recipe,
                                   boolean success) {
        if (world == null) {
            return false;
        }

        inputStack.decrement(1);
        if (inputStack.isEmpty()) {
            setStack(INPUT_SLOT, ItemStack.EMPTY);
        } else {
            setStack(INPUT_SLOT, inputStack);
        }

        if (!player.getAbilities().creativeMode) {
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

        world.updateListeners(pos, getCachedState(), getCachedState(), Block.NOTIFY_ALL);

        return success;
    }
}

package com.nekotech.block.entity;

import com.nekotech.block.entity.machines.CatNeedMachineBlockEntity;
import com.nekotech.data.AlloyFurnaceData;
import com.nekotech.item.custom.AlloyFurnace;
import com.nekotech.recipe.AlloyRecipe;
import com.nekotech.screen.AlloyFurnaceScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.text.Text;
import net.minecraft.util.Pair;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class AlloyFurnaceBlockEntity extends CatNeedMachineBlockEntity implements ExtendedScreenHandlerFactory<AlloyFurnaceData>, ImplementedInventory {

    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(5, ItemStack.EMPTY);

    public static final BooleanProperty LIT = BooleanProperty.of("lit");

    private static final int INPUT_SLOT_1 = 0; //炉槽12
    private static final int INPUT_SLOT_2 = 1;
    private static final int INPUT_SLOT_3 = 2; //燃料槽3
    private static final int OUTPUT_SLOT_1 = 3; //输出槽1
    private static final int OUTPUT_SLOT_2 = 4; //炉渣

    protected final PropertyDelegate propertyDelegate;

    private int progress=0;
    private int maxProgress=72;
    private int fuelTime = 0;          // 剩余燃料时间
    private int maxFuelTime = 0;

    @Nullable
    private AlloyRecipe.RecipeDefinition currentRecipe = null;

    public AlloyFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.basic_alloy_furnace, pos, state);

        this.propertyDelegate = new PropertyDelegate() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> progress;      // 进度
                    case 1 -> maxProgress;   // 最大进度
                    case 2 -> fuelTime;      // 剩余燃料
                    case 3 -> maxFuelTime;   // 最大燃料
                    case 4 -> currentRecipe != null ? 1 : 0;  // 是否有有效配方
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0 -> progress = value;
                    case 1 -> maxProgress = value;
                    case 2 -> fuelTime = value;
                    case 3 -> maxFuelTime = value;
                }
            }

            @Override
            public int size() {
                return 5;  // 5个属性
            }
        };
    }
    @Override
    public DefaultedList<ItemStack> getItems() {
        return inventory;
    }



    // 检查燃料槽是否可以放入物品
    @Override
    public boolean isValid(int slot, ItemStack stack) {
        if (slot == INPUT_SLOT_3) {
            // 燃料槽只接受煤炭或木炭
            return isFuel(stack);
        } else if (slot == OUTPUT_SLOT_1 || slot == OUTPUT_SLOT_2) {
            // 输出槽不允许放入物品
            return false;
        }
        return true;
    }

    // 检查是否是有效燃料
    private boolean isFuel(ItemStack stack) {
        Item item = stack.getItem();
        return item == Items.COAL || item == Items.CHARCOAL;
    }

    // 获取燃料燃烧时间
    private int getFuelTime(ItemStack fuel) {
        if (fuel.getItem() == Items.COAL) {
            return 160;  // 煤炭燃烧80秒
        } else if (fuel.getItem() == Items.CHARCOAL) {
            return 160;  // 木炭燃烧80秒
        }
        return 0;
    }


    // 主 tick 方法
    public void tick(World world, BlockPos pos, BlockState state) {
        if (world.isClient()) {
            return;
        }

        if (!canMachineRun()) {
            fuelTime = 0;
            resetProgress();
            return;
        }

        boolean shouldMarkDirty = false;

        boolean isLit = fuelTime > 0;
        boolean isCurrentlyLit = state.get(AlloyFurnace.LIT);

        // 如果发光状态发生变化
        if (isLit != isCurrentlyLit) {
            // 更新方块的 LIT 状态
            world.setBlockState(pos, state.with(AlloyFurnace.LIT, isLit), Block.NOTIFY_ALL);

            // 强制更新光照
            world.scheduleBlockRerenderIfNeeded(pos, state, state);
        }

        // 1. 检查燃料
        if (fuelTime <= 0) {
            ItemStack fuelStack = getStack(INPUT_SLOT_3);
            if (fuelStack.getCount() > 0 && getFuelTime(fuelStack)!=0) {

                fuelTime = getFuelTime(fuelStack);
                maxFuelTime = fuelTime;
                fuelStack.decrement(1);// 消耗1个燃料
                shouldMarkDirty = true;

                if (fuelStack.isEmpty()) {
                    setStack(INPUT_SLOT_3, ItemStack.EMPTY);
                }
            }
        } else {
            // 消耗燃料
            fuelTime--;
            shouldMarkDirty = true;
        }

        // 2. 检查当前配方
        AlloyRecipe.RecipeDefinition recipe = getMatchingRecipe();
        boolean hasValidRecipe = recipe != null && (fuelTime > 0 || (getStack(INPUT_SLOT_3).getItem()==Items.COAL && getStack(INPUT_SLOT_3).getCount()>0));

        if (currentRecipe != recipe) {
            currentRecipe = recipe;
            shouldMarkDirty = true;
        }

        // 3. 处理合成逻辑
        if (hasValidRecipe) {
            if (maxProgress != recipe.cookTime) {
                maxProgress = recipe.cookTime;
                shouldMarkDirty = true;
            }

            if (canCraft(recipe)) {
                progress++;
                shouldMarkDirty = true;

                if (progress >= maxProgress) {
                    craftItem(recipe);
                    resetProgress();
                }
            } else {

                resetProgress();


            }
        } else {
            resetProgress();
        }

        // 4. 标记脏数据
        if (shouldMarkDirty) {
            markDirty(world, pos, state);
        }
    }

    // 查找匹配的配方
    @Nullable
    private AlloyRecipe.RecipeDefinition getMatchingRecipe() {
        ItemStack slot1 = getStack(INPUT_SLOT_1);
        ItemStack slot2 = getStack(INPUT_SLOT_2);

        return AlloyRecipe.RecipeManager.findMatching(slot1, slot2);
    }

    // 检查是否可以合成
    private boolean canCraft(AlloyRecipe.RecipeDefinition recipe) {
        // 检查输出槽是否有空间
        ItemStack alloyOutput = recipe.alloyOutput;
        ItemStack slagOutput = recipe.slagOutput;

        boolean alloySlotOk = canInsertIntoSlot(OUTPUT_SLOT_1, alloyOutput);
        boolean slagSlotOk = slagOutput.isEmpty() || canInsertIntoSlot(OUTPUT_SLOT_2, slagOutput);

        return alloySlotOk && slagSlotOk ;
    }

    // 检查槽位是否可以插入物品
    private boolean canInsertIntoSlot(int slot, ItemStack stack) {
        ItemStack current = getStack(slot);

        if (current.isEmpty()) {
            return true;
        }

        if (!ItemStack.areItemsEqual(current, stack)) {
            return false;
        }

        int newCount = current.getCount() + stack.getCount();
        return newCount <= getMaxCountPerStack();
    }

    // 执行合成
    private void craftItem(AlloyRecipe.RecipeDefinition recipe) {
        // 消耗输入物品
        ItemStack slot1 = getStack(INPUT_SLOT_1);
        ItemStack slot2 = getStack(INPUT_SLOT_2);

        Pair<ItemStack, ItemStack> consumed = recipe.consumeInputs(slot1, slot2);
        setStack(INPUT_SLOT_1, consumed.getLeft());
        setStack(INPUT_SLOT_2, consumed.getRight());

        // 添加合金输出
        ItemStack alloyOutput = recipe.alloyOutput.copy();
        if (getStack(OUTPUT_SLOT_1).isEmpty()) {
            setStack(OUTPUT_SLOT_1, alloyOutput);
        } else {
            getStack(OUTPUT_SLOT_1).increment(alloyOutput.getCount());
        }

        // 添加炉渣输出（如果有）
        if (!recipe.slagOutput.isEmpty()) {
            ItemStack slagOutput = recipe.slagOutput.copy();
            if (getStack(OUTPUT_SLOT_2).isEmpty()) {
                setStack(OUTPUT_SLOT_2, slagOutput);
            } else {
                getStack(OUTPUT_SLOT_2).increment(slagOutput.getCount());
            }
        }
    }

    // 重置进度
    private void resetProgress() {
        if (progress != 0) {
            progress = 0;
        }
    }

    // 获取燃料进度（0-13，用于GUI显示）
    public int getFuelProgress() {
        if (maxFuelTime == 0) {
            return 0;
        }
        return (fuelTime * 13) / maxFuelTime;
    }

    // 获取合成进度（0-24，用于GUI显示）
    public int getCraftProgress() {
        if (maxProgress == 0) {
            return 0;
        }
        return (progress * 24) / maxProgress;
    }

    // 其他现有方法保持不变...
    @Override
    public Text getDisplayName() {
        return Text.translatable("container.alloy_furnace");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new AlloyFurnaceScreenHandler(syncId, playerInventory, this.propertyDelegate, this);
    }

    @Override
    public AlloyFurnaceData getScreenOpeningData(ServerPlayerEntity serverPlayerEntity) {
        return new AlloyFurnaceData(pos);
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        Inventories.writeNbt(nbt, inventory, false, registryLookup);
        nbt.putInt("progress", progress);
        nbt.putInt("maxProgress", maxProgress);
        nbt.putInt("fuelTime", fuelTime);
        nbt.putInt("maxFuelTime", maxFuelTime);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        Inventories.readNbt(nbt, inventory, registryLookup);
        progress = nbt.getInt("progress");
        maxProgress = nbt.getInt("maxProgress");
        fuelTime = nbt.getInt("fuelTime");
        maxFuelTime = nbt.getInt("maxFuelTime");
    }
}


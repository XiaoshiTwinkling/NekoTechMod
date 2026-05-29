package com.nekotech.block.entity.machines;

import com.nekotech.block.entity.CushionBlockEntity;
import com.nekotech.block.entity.ModBlockEntities;
import com.nekotech.block.entity.api.ICatNeedMachine;
import com.nekotech.item.api.googles.GoogleAbstractHUD;
import com.nekotech.block.entity.api.IHaveGoogleHUD;
import com.nekotech.item.api.googles.templates.ContainerHUDData;
import com.nekotech.item.api.googles.templates.InfoBoxHUDData;
import com.nekotech.block.ModBlocks;
import com.nekotech.recipe.AlloyPot.AlloyPotRecipeInput;
import com.nekotech.recipe.AlloyPot.AlloyRecipe;
import com.nekotech.recipe.ModRecipes;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class AlloyPotBlockEntity extends TakeFreelyMachineBlockEntity
        implements IHaveGoogleHUD, ICatNeedMachine {

    public AlloyPotBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ALLOY_POT, pos, state, 4);
    }

    private RecipeEntry<AlloyRecipe> cachedRecipe = null;

    // 客户端渲染用
    private int bounceTick = 0;
    private boolean bouncing = false;
    private boolean isCrafting = false;

    public static final int INPUT_SLOT_1 = 0;     // 输入槽1
    public static final int INPUT_SLOT_2 = 1;     // 输入槽2

    public static final int OUTPUT_SLOT_1 = 2;
    public static final int OUTPUT_SLOT_2 = 3;

    private float temperature = 0;  // 当前温度
    public int alloyProgress = 0;      // 当前合金进度
    private int alloyTimeTotal = 0;     // 总所需时间

    // 修改：改为绑定控制器位置
    private BlockPos boundControllerPos = null;

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
        boolean shouldBounce = alloyProgress > 0;

        if (bouncing) {
            bounceTick++;

            if (bounceTick >= 20) { // 1秒
                bounceTick = 0;
                bouncing = false;
            }
        } else {
            // 落地后才重新判断
            if (shouldBounce) {
                bouncing = true;
                bounceTick = 0;
            }
        }
    }

    private void serverTick() {
        updateTemperature();

        if (isInputEmpty()) {
            isCrafting = false;
            cachedRecipe = null;
            resetProgress();
            return;
        }

        if (cachedRecipe == null) {
            isCrafting = false;
            cachedRecipe = getCurrentRecipe().orElse(null);
            resetProgress();
        }

        if (cachedRecipe == null) {
            isCrafting = false;
            return;
        }

        AlloyRecipe recipe = cachedRecipe.value();

        if (!canWork(recipe)) {
            isCrafting = false;
            return;
        }

        isCrafting = true;

        alloyTimeTotal = recipe.cookTime();
        alloyProgress++;

        if (alloyProgress >= alloyTimeTotal) {
            craft(recipe);
            cachedRecipe = null;
            world.playSound(null, pos, net.minecraft.sound.SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                    net.minecraft.sound.SoundCategory.BLOCKS, 0.6f, 1.2f);
            resetProgress();
        }

        markDirty();

        // 定期清理无效绑定
        if (world.getTime() % 200 == 0) {
            cleanupInvalidBindings();
        }
    }

    private boolean canWork(AlloyRecipe recipe) {
        // 使用新的 ICatNeedMachine 接口方法
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
        return belowState.isOf(ModBlocks.HEATER);
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

    public int getAlloyTimeTotal() {
        return alloyTimeTotal;
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

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.putInt("Progress", alloyProgress);

        if (boundControllerPos != null) {
            nbt.putInt("ControllerX", boundControllerPos.getX());
            nbt.putInt("ControllerY", boundControllerPos.getY());
            nbt.putInt("ControllerZ", boundControllerPos.getZ());
        }
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        alloyProgress = nbt.getInt("Progress");

        if (nbt.contains("ControllerX")) {
            boundControllerPos = new BlockPos(
                    nbt.getInt("ControllerX"),
                    nbt.getInt("ControllerY"),
                    nbt.getInt("ControllerZ")
            );
        } else {
            boundControllerPos = null;
        }
    }

    @Override
    public List<GoogleAbstractHUD> getGoogleHUDs(World world, BlockPos pos, BlockState state) {
        if (world.isClient()) {
            return null;
        }

        List<GoogleAbstractHUD> huds = new ArrayList<>();

        List<ItemStack> items = new ArrayList<>();
        if (this instanceof Inventory inventory) {
            for (int i = 0; i < inventory.size(); i++) {
                ItemStack stack = inventory.getStack(i);
                items.add(stack.copy());
            }
        }
        Text containerTitle = Text.translatable("block.neko-technology.alloy_pot");
        ContainerHUDData containerHUD = new ContainerHUDData(pos, items, containerTitle, 2, 2);
        huds.add(containerHUD);

        boolean hasActiveController = hasActiveController();

        Text title = Text.translatable("block.neko-technology.alloy_pot").formatted(Formatting.GOLD);
        Text content = Text.translatable("block.neko-technology.alloy_pot.description"
                ,(int) getHeaterTemperature()
                ,getHeaterMaxTemperature()
                ,isCrafting ? Text.translatable("block.neko-technology.yes") : Text.translatable("block.neko-technology.no")
                ,isHeater(getWorld().getBlockState(getPos().down())) && hasActiveController ?
                        Text.translatable("block.neko-technology.yes") : Text.translatable("block.neko-technology.no")
        );
        huds.add(new InfoBoxHUDData(pos, title, content));

        return huds;
    }

    @Override
    public @Nullable BlockPos getBoundControllerPos() {
        return boundControllerPos;
    }

    @Override
    public void setBoundControllerPos(@Nullable BlockPos pos) {
        this.boundControllerPos = pos;
        this.markDirty();
    }

}
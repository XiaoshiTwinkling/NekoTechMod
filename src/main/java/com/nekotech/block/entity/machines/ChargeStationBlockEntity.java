package com.nekotech.block.entity.machines;

import com.nekotech.block.entity.ModBlockEntities;
import com.nekotech.block.entity.api.ICatNeedMachine;
import com.nekotech.block.entity.api.IHaveGoogleHUD;
import com.nekotech.block.entity.api.ImplementedInventory;
import com.nekotech.block.entity.api.TakeFreelyInventory;
import com.nekotech.block.entity.api.electrical.ITransferElectrical;
import com.nekotech.block.entity.machines.coil.CoilBlockEntity;
import com.nekotech.item.api.chargeable_item.IChargeableItem;
import com.nekotech.item.api.googles.GoogleAbstractHUD;
import com.nekotech.item.api.googles.templates.ContainerHUDData;
import com.nekotech.item.api.googles.templates.InfoBoxHUDData;
import com.nekotech.recipe.ChargeStation.ChargeRecipe;
import com.nekotech.recipe.ModRecipes;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.input.SingleStackRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ChargeStationBlockEntity extends TakeFreelyMachineBlockEntity
        implements TakeFreelyInventory, IHaveGoogleHUD, ICatNeedMachine, ImplementedInventory, ITransferElectrical {

    public static final int SLOT = 0;
    public static final int INVENTORY_SIZE = 1;
    /** 每 tick 充入/消耗的能量单位 */
    private static final float ENERGY_PER_TICK = 1.0f;

    private int chargeProgress = 0;
    private int chargeTimeTotal = 0;
    private boolean isCharging = false;

    private RecipeEntry<ChargeRecipe> cachedRecipe = null;

    private boolean isChargeableMode = false;
    private float targetFlux = 0; // 充能目标值（最大能量）

    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);

    public ChargeStationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHARGE_STATION, pos, state, INVENTORY_SIZE);
    }


    public static void tick(World world, BlockPos pos, BlockState state, ChargeStationBlockEntity blockEntity) {
        if (world.isClient) {
            blockEntity.clientTick();
        } else {
            blockEntity.serverTick();
        }
    }

    public void clientTick() {
        if (isCharging && world != null && world.random.nextFloat() < 0.3f) {
            spawnChargeParticles();
        }
    }

    private void serverTick() {
        ItemStack inputStack = getStack(SLOT);

        // 没有物品时重置
        if (inputStack.isEmpty()) {
            resetCharging();
            return;
        }

        // 检查下方是否有正在运行的满铜丝 CoilBlock
        boolean hasActiveCoil = hasActiveCoilBelow();

        if (!hasActiveCoil) {
            if (isCharging) {
                resetCharging();
                markDirty();
            }
            return;
        }

        // 判断物品类型：可充能物品 或 配方物品
        if (inputStack.getItem() instanceof IChargeableItem chargeable) {
            handleChargeableItem(inputStack, chargeable);
        } else {
            handleRecipeItem(inputStack);
        }

        markDirty();
    }


    private void handleChargeableItem(ItemStack stack, IChargeableItem chargeable) {
        float currentFlux = chargeable.getNekoFlux(stack);
        float maxFlux = chargeable.getMaxNekoFlux(stack);

        if (currentFlux >= maxFlux) {
            resetCharging();
            return;
        }

        if (!isCharging || !isChargeableMode) {
            isChargeableMode = true;
            isCharging = true;
            chargeProgress = 0;
            targetFlux = maxFlux;
            chargeTimeTotal = (int) (maxFlux - currentFlux); // 剩余需要充能的量
            cachedRecipe = null; // 清除配方缓存
        }

        CoilBlockEntity coil = getCoilBelow();
        if (coil == null || coil.getNekoFlux() < ENERGY_PER_TICK) {
            if (isCharging) {
                resetCharging();
            }
            return;
        }

        coil.setNekoFlux(coil.getNekoFlux() - ENERGY_PER_TICK);

        float newFlux = Math.min(maxFlux, currentFlux + ENERGY_PER_TICK);
        chargeable.setNekoFlux(stack, newFlux);
        chargeProgress++;

        if (world instanceof ServerWorld serverWorld && chargeProgress % 10 == 0) {
            serverWorld.spawnParticles(ParticleTypes.ELECTRIC_SPARK,
                    pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                    3, 0.2, 0.1, 0.2, 0.05);
        }

        if (newFlux >= maxFlux) {
            completeCharging();
        }
    }


    private void handleRecipeItem(ItemStack inputStack) {
        // 如果之前是充电模式，重置
        if (isChargeableMode) {
            resetCharging();
        }

        // 缓存配方
        if (cachedRecipe == null) {
            cachedRecipe = findMatchingRecipe(inputStack).orElse(null);
            if (cachedRecipe == null) {
                resetCharging();
                return;
            }
            chargeTimeTotal = cachedRecipe.value().getChargeTime();
            isCharging = true;
            chargeProgress = 0;
        }

        // 开始/继续充能
        isCharging = true;
        chargeProgress++;

        // 粒子效果
        if (world instanceof ServerWorld serverWorld && chargeProgress % 10 == 0) {
            serverWorld.spawnParticles(ParticleTypes.ELECTRIC_SPARK,
                    pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                    3, 0.2, 0.1, 0.2, 0.05);
        }

        // 充能完成
        if (chargeProgress >= chargeTimeTotal) {
            completeCharging();
        }
    }


    private void completeCharging() {
        if (world == null) return;

        if (isChargeableMode) {
            world.playSound(null, pos,
                    net.minecraft.sound.SoundEvents.BLOCK_BEACON_ACTIVATE,
                    net.minecraft.sound.SoundCategory.BLOCKS, 0.6f, 1.2f);
            if (world instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(ParticleTypes.END_ROD,
                        pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                        15, 0.3, 0.2, 0.3, 0.1);
            }
        } else {
            if (cachedRecipe == null) return;
            ItemStack output = cachedRecipe.value().getResult(world.getRegistryManager()).copy();
            setStack(SLOT, output);

            world.playSound(null, pos,
                    net.minecraft.sound.SoundEvents.BLOCK_BEACON_ACTIVATE,
                    net.minecraft.sound.SoundCategory.BLOCKS, 0.6f, 1.2f);
            if (world instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(ParticleTypes.END_ROD,
                        pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                        15, 0.3, 0.2, 0.3, 0.1);
            }
        }

        resetCharging();
    }

    private void resetCharging() {
        chargeProgress = 0;
        chargeTimeTotal = 0;
        cachedRecipe = null;
        isCharging = false;
        isChargeableMode = false;
        targetFlux = 0;
    }


    private boolean hasActiveCoilBelow() {
        CoilBlockEntity coil = getCoilBelow();
        if (coil == null) return false;
        if (coil.getCoilCounts()[0] != 6) return false; // 铜线圈必须满6个
        return coil.isWorking();
    }

    @Nullable
    private CoilBlockEntity getCoilBelow() {
        if (world == null) return null;
        BlockPos belowPos = pos.down();
        BlockEntity below = world.getBlockEntity(belowPos);
        if (below instanceof CoilBlockEntity coil) {
            return coil;
        }
        return null;
    }

    private Optional<RecipeEntry<ChargeRecipe>> findMatchingRecipe(ItemStack stack) {
        if (world == null) return Optional.empty();
        SingleStackRecipeInput input = new SingleStackRecipeInput(stack);
        return world.getRecipeManager()
                .getFirstMatch(ModRecipes.CHARGE_RECIPE_TYPE, input, world);
    }


    @Override
    public boolean canInsert(ItemStack stack, int slot) {
        if (!getStack(slot).isEmpty()) return false;
        return true;
    }

    @Override
    public boolean canExtract(ItemStack stack, int slot) {
        return true;
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        return new int[]{SLOT};
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
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.putInt("ChargeProgress", chargeProgress);
        nbt.putInt("ChargeTimeTotal", chargeTimeTotal);
        nbt.putBoolean("IsCharging", isCharging);
        nbt.putBoolean("IsChargeableMode", isChargeableMode);
        nbt.putFloat("TargetFlux", targetFlux);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        chargeProgress = nbt.getInt("ChargeProgress");
        chargeTimeTotal = nbt.getInt("ChargeTimeTotal");
        isCharging = nbt.getBoolean("IsCharging");
        isChargeableMode = nbt.getBoolean("IsChargeableMode");
        targetFlux = nbt.getFloat("TargetFlux");
    }

    @Override
    public List<GoogleAbstractHUD> getGoogleHUDs(World world, BlockPos pos, BlockState state) {
        if (world.isClient()) return null;

        List<GoogleAbstractHUD> huds = new ArrayList<>();

        ItemStack stack = getStack(SLOT);
        Text title = Text.translatable("block.neko-technology.charge_station").formatted(Formatting.GOLD);

        StringBuilder info = new StringBuilder();
        info.append(Text.translatable("hud.charge_station.item",
                stack.isEmpty() ? Text.translatable("hud.empty") : stack.getName()).getString());
        info.append("\n");

        boolean hasCoil = hasActiveCoilBelow();
        info.append(Text.translatable("hud.charge_station.coil_status",
                hasCoil ? Text.translatable("hud.active").formatted(Formatting.GREEN)
                        : Text.translatable("hud.inactive").formatted(Formatting.RED)).getString());
        info.append("\n");

        if (isCharging && chargeTimeTotal > 0) {
            int percent = (int) ((float) chargeProgress / chargeTimeTotal * 100);
            info.append(Text.translatable("hud.charge_station.progress", percent).getString());
        } else if (!stack.isEmpty() && hasCoil) {
            info.append(Text.translatable("hud.charge_station.waiting").getString());
        }

        Text content = Text.literal(info.toString());
        huds.add(new InfoBoxHUDData(pos, title, content));

        List<ItemStack> items = new ArrayList<>();
        if (this instanceof Inventory inventory) {
            for (int i = 0; i < inventory.size(); i++) {
                items.add(inventory.getStack(i).copy());
            }
        }
        Text containerTitle = Text.translatable("block.neko-technology.charge_station");
        ContainerHUDData containerHUD = new ContainerHUDData(pos, items, containerTitle, 1, 1);
        huds.add(containerHUD);

        return huds;
    }


    @Override
    public void markRemoved() {
        if (world != null && !world.isClient) {
            ItemStack stack = getStack(SLOT);
            if (!stack.isEmpty()) {
                ItemScatterer.spawn(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
            }
        }
        super.markRemoved();
    }


    public int getChargeProgress() { return chargeProgress; }
    public int getChargeTimeTotal() { return chargeTimeTotal; }
    public boolean isCharging() { return isCharging; }
    public boolean isChargeableMode() { return isChargeableMode; }


    private void spawnChargeParticles() {
        if (world == null) return;
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.8;
        double z = pos.getZ() + 0.5;
        for (int i = 0; i < 3; i++) {
            double offsetX = (world.random.nextDouble() - 0.5) * 0.8;
            double offsetY = world.random.nextDouble() * 0.3;
            double offsetZ = (world.random.nextDouble() - 0.5) * 0.8;
            world.addParticle(ParticleTypes.WHITE_ASH,
                    x + offsetX, y + offsetY, z + offsetZ,
                    0, 0.02, 0);
        }
    }


    @Override
    public DefaultedList<ItemStack> getItems() {
        return items;
    }


    @Override
    public @Nullable BlockPos getBoundControllerPos() {
        return null;
    }

    @Override
    public void setBoundControllerPos(@Nullable BlockPos pos) {
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        super.setStack(slot, stack);
        if (stack.isEmpty()) {
            resetCharging();
            markDirty();
        }
    }

    @Override
    public boolean handleRightClick(PlayerEntity player, ItemStack stack) {
        if (!getStack(SLOT).isEmpty()) {
            boolean result = takeOutAllItems(player);
            return result;
        }
        return putInItem(player, stack);
    }
}
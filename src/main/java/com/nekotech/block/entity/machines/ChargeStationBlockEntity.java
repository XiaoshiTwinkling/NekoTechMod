package com.nekotech.block.entity.machines;

import com.nekotech.block.entity.ModBlockEntities;
import com.nekotech.block.entity.api.ICatNeedMachine;
import com.nekotech.block.entity.api.IHaveGoogleHUD;
import com.nekotech.block.entity.api.ImplementedInventory;
import com.nekotech.block.entity.api.TakeFreelyInventory;
import com.nekotech.block.entity.machines.coil.CoilBlockEntity;
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
        implements TakeFreelyInventory, IHaveGoogleHUD, ICatNeedMachine, ImplementedInventory {
    public static final int SLOT = 0;
    public static final int INVENTORY_SIZE = 1;

    private int chargeProgress = 0;
    private int chargeTimeTotal = 0;
    private RecipeEntry<ChargeRecipe> cachedRecipe = null;
    private boolean isCharging = false;

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
            // Coil 停止工作，重置进度
            if (isCharging) {
                resetCharging();
                markDirty();
            }
            return;
        }

        // 缓存配方
        if (cachedRecipe == null) {
            cachedRecipe = findMatchingRecipe(inputStack).orElse(null);
            if (cachedRecipe == null) {
                resetCharging();
                return;
            }
            chargeTimeTotal = cachedRecipe.value().getChargeTime();
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

        markDirty();
    }

    // ========== 充能流程 ==========

    private void completeCharging() {
        if (cachedRecipe == null || world == null) return;

        ItemStack output = cachedRecipe.value().getResult(world.getRegistryManager()).copy();
        setStack(SLOT, output);

        // 播放完成音效
        world.playSound(null, pos,
                net.minecraft.sound.SoundEvents.BLOCK_BEACON_ACTIVATE,
                net.minecraft.sound.SoundCategory.BLOCKS, 0.6f, 1.2f);

        if (world instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(ParticleTypes.END_ROD,
                    pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    15, 0.3, 0.2, 0.3, 0.1);
        }

        resetCharging();
    }

    private void resetCharging() {
        chargeProgress = 0;
        chargeTimeTotal = 0;
        cachedRecipe = null;
        isCharging = false;
    }


    private boolean hasActiveCoilBelow() {
        if (world == null) return false;

        BlockPos belowPos = pos.down();
        BlockEntity below = world.getBlockEntity(belowPos);

        if (!(below instanceof CoilBlockEntity coil)) return false;

        if(coil.getCoilCounts()[0] != 6) return false;

        return coil.isWorking();
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
        return true; // 可以取出任何物品
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
    public boolean handleRightClick(PlayerEntity player, ItemStack stack) {
        if (!getStack(SLOT).isEmpty()) {
            if (player.isSneaking()) {
                return takeOutAllItems(player);
            }
            return takeOutAllItems(player);
        }

        return putInItem(player, stack);
    }


    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.putInt("ChargeProgress", chargeProgress);
        nbt.putInt("ChargeTimeTotal", chargeTimeTotal);
        nbt.putBoolean("IsCharging", isCharging);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        chargeProgress = nbt.getInt("ChargeProgress");
        chargeTimeTotal = nbt.getInt("ChargeTimeTotal");
        isCharging = nbt.getBoolean("IsCharging");
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
                stack = inventory.getStack(i);
                items.add(stack.copy()); // 复制一份，避免修改原物品
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

    public int getChargeProgress() {
        return chargeProgress;
    }

    public int getChargeTimeTotal() {
        return chargeTimeTotal;
    }

    public boolean isCharging() {
        return isCharging;
    }

    private void spawnChargeParticles() {
        if (world == null) return;

        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.8;
        double z = pos.getZ() + 0.5;

        for (int i = 0; i < 3; i++) {
            double offsetX = (world.random.nextDouble() - 0.5) * 0.8;
            double offsetY = world.random.nextDouble() * 0.3;
            double offsetZ = (world.random.nextDouble() - 0.5) * 0.8;

            world.addParticle(
                    ParticleTypes.WHITE_ASH,
                    x + offsetX, y + offsetY, z + offsetZ,
                    0, 0.02, 0
            );
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
}

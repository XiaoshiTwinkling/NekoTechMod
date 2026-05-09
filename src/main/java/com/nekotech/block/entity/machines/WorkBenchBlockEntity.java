package com.nekotech.block.entity.machines;

import com.nekotech.block.entity.ModBlockEntities;
import com.nekotech.block.entity.api.TakeFreelyInventory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

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
            ItemStack current = inventory.get(slot);
            if (!current.equals(stack)) {
                inventory.set(slot, stack);
                markDirty();

                if (world != null && !world.isClient) {
                    world.updateListeners(pos, getCachedState(), getCachedState(),
                            Block.NOTIFY_ALL | Block.REDRAW_ON_MAIN_THREAD);
                }
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
        markDirty();
        if (world != null && !world.isClient) {
            world.updateListeners(pos, getCachedState(), getCachedState(), Block.NOTIFY_LISTENERS);
        }
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
        markDirty();
        if (world != null && !world.isClient) {
            world.updateListeners(pos, getCachedState(), getCachedState(), Block.NOTIFY_LISTENERS);
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
        Inventories.writeNbt(nbt, inventory, registryLookup);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        Inventories.readNbt(nbt, inventory, registryLookup);
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registryLookup) {
        return createNbt(registryLookup);
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
}

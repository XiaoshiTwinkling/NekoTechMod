package com.nekotech.block.entity;

import com.nekotech.block.entity.api.ImplementedInventory;
import com.nekotech.item.api.googles.IHaveGoogleHUD;
import com.nekotech.block.ModBlocks;
import com.nekotech.util.ElevatorMath;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class ElevatorCoreBlockEntity extends BlockEntity implements ImplementedInventory, NamedScreenHandlerFactory, IHaveGoogleHUD {
    public static final int INVENTORY_SIZE = 27;
    public static final int MAX_VERTICAL_DISTANCE = 20;
    public static final int TICKS_PER_FLOOR = 12;

    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);

    /**
     * 总方块高度。两点垂直距离最大 20 时，height 最大是 21。
     */
    private int height = 1;

    private int currentFloor = 0;
    private int startFloor = 0;
    private int targetFloor = 0;

    private long moveStartTick = 0L;
    private int moveDurationTicks = 0;
    private boolean moving = false;

    /**
     * 只用于防止拆结构时递归调用，不写入 NBT。
     */
    private boolean destroying = false;

    public ElevatorCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ELEVATOR_CORE_BLOCK_ENTITY, pos, state);
    }

    public static void serverTick(World world, BlockPos pos, BlockState state, ElevatorCoreBlockEntity be) {
        if (!be.moving) {
            return;
        }

        long elapsed = world.getTime() - be.moveStartTick;

        if (elapsed >= be.moveDurationTicks) {
            be.currentFloor = be.targetFloor;
            be.startFloor = be.targetFloor;
            be.moving = false;
            be.moveDurationTicks = 0;
            be.sync();
        }
    }

    public void initializeStructure(int height) {
        this.height = Math.max(1, Math.min(height, MAX_VERTICAL_DISTANCE + 1));
        this.currentFloor = 0;
        this.startFloor = 0;
        this.targetFloor = 0;
        this.moving = false;
        this.moveDurationTicks = 0;
        sync();
    }

    public boolean isDestroying() {
        return destroying;
    }

    public int getHeight() {
        return height;
    }

    public int getCurrentFloor() {
        return currentFloor;
    }

    public int getTargetFloor() {
        return targetFloor;
    }

    public boolean isMoving() {
        return moving;
    }

    public boolean isValidFloor(int floor) {
        return floor >= 0 && floor < height;
    }

    public boolean isCabinDockedAt(int floor) {
        return !moving && currentFloor == floor;
    }

    public void handleFloorClick(PlayerEntity player, int clickedFloor) {
        if (world == null || world.isClient) {
            return;
        }

        if (!isValidFloor(clickedFloor)) {
            return;
        }

        if (moving) {
            return;
        }

        if (isCabinDockedAt(clickedFloor)) {
            player.openHandledScreen(this);
        } else {
            startMoveTo(clickedFloor);
        }
    }

    public void startMoveTo(int floor) {
        if (world == null || world.isClient) {
            return;
        }

        if (!isValidFloor(floor)) {
            return;
        }

        if (moving) {
            return;
        }

        if (currentFloor == floor) {
            return;
        }

        this.startFloor = this.currentFloor;
        this.targetFloor = floor;
        this.moveStartTick = world.getTime();
        this.moveDurationTicks = Math.max(1, Math.abs(targetFloor - startFloor) * TICKS_PER_FLOOR);
        this.moving = true;

        sync();
    }

    /**
     * 服务端和客户端都用同一套公式。
     * 服务端通常用 tickDelta = 0。
     * BER 用 tickDelta 做帧间插值。
     */
    public double getCabinFloor(float tickDelta) {
        if (world == null) {
            return currentFloor;
        }

        if (!moving) {
            return currentFloor;
        }

        double elapsed = world.getTime() + tickDelta - moveStartTick;
        double progress = ElevatorMath.clamp(elapsed / moveDurationTicks, 0.0D, 1.0D);
        double eased = ElevatorMath.easeInOut(progress);

        return ElevatorMath.lerp(startFloor, targetFloor, eased);
    }

    public void destroyWholeStructure(boolean dropInventory, boolean includeCore) {
        if (world == null || world.isClient || destroying) {
            return;
        }

        destroying = true;

        if (dropInventory) {
            ItemScatterer.spawn(world, pos, this);
        }

        int h = Math.max(1, height);

        for (int y = h - 1; y >= 0; y--) {
            if (y == 0 && !includeCore) {
                continue;
            }

            BlockPos p = pos.up(y);
            BlockState state = world.getBlockState(p);

            if (state.isOf(ModBlocks.ELEVATOR_CORE_BLOCK) || state.isOf(ModBlocks.ELEVATOR_PART_BLOCK)) {
                world.setBlockState(p, Blocks.AIR.getDefaultState(), 3);
            }
        }

        markDirty();
    }

    public void sync() {
        markDirty();

        if (world instanceof ServerWorld serverWorld) {
            serverWorld.getChunkManager().markForUpdate(pos);
        }
    }

    @Override
    public DefaultedList<ItemStack> getItems() {
        return items;
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        if (world == null || world.getBlockEntity(pos) != this) {
            return false;
        }

        /*
         * 因为 GUI 可以从顶部附属方块打开，不能用箱子默认 8 格距离。
         * 这里给 32 格，足够覆盖 20 格高结构。
         */
        return player.squaredDistanceTo(
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D
        ) <= 32.0D * 32.0D;
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("container.neko-technology.elevator");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, net.minecraft.entity.player.PlayerInventory playerInventory, PlayerEntity player) {
        return GenericContainerScreenHandler.createGeneric9x3(syncId, playerInventory, this);
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);

        Inventories.writeNbt(nbt, items, registryLookup);
        writeClientFields(nbt);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);

        if (nbt.contains("Items")) {
            Inventories.readNbt(nbt, items, registryLookup);
        }

        readClientFields(nbt);
    }

    private void writeClientFields(NbtCompound nbt) {
        nbt.putInt("Height", height);
        nbt.putInt("CurrentFloor", currentFloor);
        nbt.putInt("StartFloor", startFloor);
        nbt.putInt("TargetFloor", targetFloor);
        nbt.putLong("MoveStartTick", moveStartTick);
        nbt.putInt("MoveDurationTicks", moveDurationTicks);
        nbt.putBoolean("Moving", moving);
    }

    private void readClientFields(NbtCompound nbt) {
        height = Math.max(1, Math.min(nbt.getInt("Height"), MAX_VERTICAL_DISTANCE + 1));
        currentFloor = nbt.getInt("CurrentFloor");
        startFloor = nbt.getInt("StartFloor");
        targetFloor = nbt.getInt("TargetFloor");
        moveStartTick = nbt.getLong("MoveStartTick");
        moveDurationTicks = nbt.getInt("MoveDurationTicks");
        moving = nbt.getBoolean("Moving");
    }

    private NbtCompound createClientNbt() {
        NbtCompound nbt = new NbtCompound();
        writeClientFields(nbt);
        return nbt;
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registryLookup) {
        return createClientNbt();
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public boolean shouldShowHUD(World world, BlockPos pos, BlockState state) {
        return false;
    }
}

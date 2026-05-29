package com.nekotech.block.entity;

import com.nekotech.block.entity.api.ICatControlBlock;
import com.nekotech.block.entity.api.ICatNeedMachine;
import com.nekotech.item.api.googles.GoogleAbstractHUD;
import com.nekotech.block.entity.api.IHaveGoogleHUD;
import com.nekotech.item.api.googles.templates.InfoBoxHUDData;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CushionBlockEntity extends BlockEntity implements ICatControlBlock, IHaveGoogleHUD {

    private static final int MAX_MACHINES = 4;
    private static final int CHECK_INTERVAL = 10;
    private static final String CONTROLLER_TYPE = "cushion";

    private boolean hasCat = false;
    private long lastCheckTime = 0;
    private final String controllerId;

    private final List<BlockPos> controlledMachines = new ArrayList<>();

    public CushionBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CUSHION, pos, state);
        this.controllerId = "cushion_" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Override
    public String getControllerId() {
        return controllerId;
    }

    @Override
    public boolean isControllerActive() {
        return hasCatCached();
    }

    @Override
    public int getMaxControlledMachines() {
        return MAX_MACHINES;
    }

    @Override
    public boolean canControlMachine(World world, BlockPos machinePos, BlockState machineState) {
        // 猫猫坐垫只控制需要猫的机器
        BlockEntity be = world.getBlockEntity(machinePos);
        return be instanceof ICatNeedMachine;
    }

    @Override
    public void onControllerStateChanged(World world, BlockPos pos, BlockState state, boolean newState) {
        // 当猫的状态变化时，通知所有控制的机器
        for (BlockPos machinePos : controlledMachines) {
            BlockEntity be = world.getBlockEntity(machinePos);
            if (be instanceof ICatNeedMachine machine) {
                // 可以在这里添加状态变化通知
            }
        }
    }

    @Override
    public List<BlockPos> getControlledMachines() {
        return new ArrayList<>(controlledMachines);
    }

    @Override
    public boolean registerMachine(BlockPos machinePos) {
        if (controlledMachines.contains(machinePos)) {
            return true;
        }

        if (controlledMachines.size() >= MAX_MACHINES) {
            return false;
        }

        controlledMachines.add(machinePos);
        markDirty();
        return true;
    }

    @Override
    public boolean unregisterMachine(BlockPos machinePos) {
        boolean removed = controlledMachines.remove(machinePos);
        if (removed) {
            markDirty();
        }
        return removed;
    }

    @Override
    public boolean isMachineRegistered(BlockPos machinePos) {
        return controlledMachines.contains(machinePos);
    }

    @Override
    public void cleanupInvalidMachines() {
        if (world == null || world.isClient) return;

        controlledMachines.removeIf(machinePos -> {
            BlockEntity be = world.getBlockEntity(machinePos);
            return be == null || be.isRemoved() || !(be instanceof ICatNeedMachine);
        });

        if (!controlledMachines.isEmpty()) {
            markDirty();
        }
    }

    @Override
    public String getControllerType() {
        return CONTROLLER_TYPE;
    }

    public static void tick(World world, BlockPos pos, BlockState state, CushionBlockEntity be) {
        if (world.isClient) return;

        if (world.getTime() - be.lastCheckTime >= CHECK_INTERVAL) {
            be.lastCheckTime = world.getTime();
            boolean hadCat = be.hasCat;
            be.hasCat = be.detectCat(world, pos);

            // 猫的状态变化时触发回调
            if (hadCat != be.hasCat) {
                be.onControllerStateChanged(world, pos, state, be.hasCat);
            }
        }

        if (world.getTime() % 40 == 0) {
            be.cleanupInvalidMachines();
        }
    }

    private boolean detectCat(World world, BlockPos pos) {
        Box box = new Box(
                pos.getX(), pos.getY() + 1, pos.getZ(),
                pos.getX() + 1, pos.getY() + 2, pos.getZ() + 1
        );

        return !world.getEntitiesByClass(
                CatEntity.class,
                box,
                CatEntity::isAlive
        ).isEmpty();
    }

    public boolean hasCatCached() {
        return hasCat;
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.putBoolean("HasCat", hasCat);
        nbt.putLong("LastCheckTime", lastCheckTime);
        nbt.putString("ControllerId", controllerId);

        // 保存控制的机器位置
        NbtList machineList = new NbtList();
        for (BlockPos pos : controlledMachines) {
            NbtCompound posNbt = new NbtCompound();
            posNbt.putInt("X", pos.getX());
            posNbt.putInt("Y", pos.getY());
            posNbt.putInt("Z", pos.getZ());
            machineList.add(posNbt);
        }
        nbt.put("ControlledMachines", machineList);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        hasCat = nbt.getBoolean("HasCat");
        lastCheckTime = nbt.getLong("LastCheckTime");
        // controllerId 在构造函数中生成，不需要读取

        // 读取控制的机器位置
        controlledMachines.clear();
        NbtList machineList = nbt.getList("ControlledMachines", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < machineList.size(); i++) {
            NbtCompound posNbt = machineList.getCompound(i);
            BlockPos pos = new BlockPos(
                    posNbt.getInt("X"),
                    posNbt.getInt("Y"),
                    posNbt.getInt("Z")
            );
            controlledMachines.add(pos);
        }
    }

    @Override
    public List<GoogleAbstractHUD> getGoogleHUDs(World world, BlockPos pos, BlockState state) {
        if (world.isClient()) {
            return null;
        }

        List<GoogleAbstractHUD> huds = new ArrayList<>();

        Text title = Text.translatable("block.neko-technology.cushion_block").formatted(Formatting.GOLD);
        Text content = Text.translatable("block.neko-technology.cushion.description",
                hasCatCached() ? Text.translatable("block.neko-technology.yes") : Text.translatable("block.neko-technology.no"),
                controlledMachines.size(),
                MAX_MACHINES
        );
        huds.add(new InfoBoxHUDData(pos, title, content));

        return huds;
    }
}
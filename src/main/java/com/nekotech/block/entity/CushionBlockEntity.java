package com.nekotech.block.entity;

import com.nekotech.block.entity.machines.CatNeedMachineBlockEntity;
import com.nekotech.item.api.googles.GoogleAbstractHUD;
import com.nekotech.item.api.googles.IHaveGoogleHUD;
import com.nekotech.item.api.googles.templates.ContainerHUDData;
import com.nekotech.item.api.googles.templates.InfoBoxHUDData;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class CushionBlockEntity extends BlockEntity implements IHaveGoogleHUD {

    private static final int MAX_MACHINES = 4;
    private static final int CHECK_INTERVAL = 10;

    private boolean hasCat = false;
    private long lastCheckTime = 0;

    private final CatNeedMachineBlockEntity[] machines = new CatNeedMachineBlockEntity[MAX_MACHINES];
    private int machineCount = 0;

    public CushionBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.cushion, pos, state);
    }

    public static void tick(World world, BlockPos pos, BlockState state, CushionBlockEntity be) {
        if (world.isClient) return;

        if (world.getTime() - be.lastCheckTime >= CHECK_INTERVAL) {
            be.lastCheckTime = world.getTime();
            be.hasCat = be.detectCat(world, pos);
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

    public boolean tryRegister(CatNeedMachineBlockEntity machine) {
        if (machine == null) return false;

        // 已注册
        if (isRegistered(machine)) return true;

        if (machineCount >= MAX_MACHINES) return false;

        for (int i = 0; i < MAX_MACHINES; i++) {
            if (machines[i] == null) {
                machines[i] = machine;
                machineCount++;
                return true;
            }
        }

        return false;
    }

    public void unregisterMachine(CatNeedMachineBlockEntity machine) {
        if (machine == null) return;

        for (int i = 0; i < MAX_MACHINES; i++) {
            if (machines[i] == machine) {
                machines[i] = null;
                machineCount--;
                return;
            }
        }
    }

    public boolean isRegistered(CatNeedMachineBlockEntity machine) {
        for (CatNeedMachineBlockEntity m : machines) {
            if (m == machine) return true;
        }
        return false;
    }

    public boolean isFull() {
        return machineCount >= MAX_MACHINES;
    }

    public int getMachineCount() {
        return machineCount;
    }


    private void cleanupInvalidMachines() {
        for (int i = 0; i < MAX_MACHINES; i++) {
            CatNeedMachineBlockEntity m = machines[i];

            if (m == null) continue;

            // world不同 or 已移除
            if (m.isRemoved() || m.getWorld() != this.world) {
                machines[i] = null;
                if (machineCount > 0) {
                    machineCount--;
                }
            }
        }
    }

    @Override
    public void markRemoved() {
        super.markRemoved();

        for (int i = 0; i < MAX_MACHINES; i++) {
            machines[i] = null;
        }
        machineCount = 0;
    }

    @Override
    public List<GoogleAbstractHUD> getGoogleHUDs(World world, BlockPos pos, BlockState state) {
        // 只在服务端返回数据
        if (world.isClient()) {
            return null;
        }

        java.util.List<GoogleAbstractHUD> huds = new java.util.ArrayList<>();

        Text title = Text.translatable("block.neko-technology.cushion_block").formatted(Formatting.GOLD);
        Text content = Text.translatable("block.neko-technology.cushion.description"
                , hasCatCached() ? Text.translatable("block.neko-technology.yes") : Text.translatable("block.neko-technology.no")
        );
        huds.add(new InfoBoxHUDData(pos, title, content));

        return huds;
    }
}
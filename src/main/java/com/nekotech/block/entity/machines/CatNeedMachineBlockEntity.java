package com.nekotech.block.entity.machines;

import com.nekotech.NekoTechnology;
import com.nekotech.block.entity.CushionBlockEntity;
import com.nekotech.item.block.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.List;

/*
这是模组中所有需要猫猫管理的机器的基类
 */

public abstract class CatNeedMachineBlockEntity extends BlockEntity {

    private BlockPos boundCushion = null;

    public CatNeedMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public boolean canMachineRun() {
        if (world == null || world.isClient) return false;

        CushionBlockEntity cushion = getBoundCushion();

        if (cushion != null) {
            return cushion.hasCatCached();
        }

        cushion = findAndBindCushion();

        return cushion != null && cushion.hasCatCached();
    }

    private CushionBlockEntity getBoundCushion() {
        if (boundCushion == null) return null;

        BlockEntity be = world.getBlockEntity(boundCushion);

        if (be instanceof CushionBlockEntity cushion) {
            if (cushion.isRegistered(this)) {
                return cushion;
            }
        }

        boundCushion = null;
        return null;
    }

    private CushionBlockEntity findAndBindCushion() {
        CushionBlockEntity cushion = findAvailableCushion();

        if (cushion == null) return null;

        if (cushion.tryRegister(this)) {
            boundCushion = cushion.getPos();
            return cushion;
        }

        return null;
    }

    @Override
    public void markRemoved() {
        super.markRemoved();

        if (world == null || boundCushion == null) return;

        BlockEntity be = world.getBlockEntity(boundCushion);

        if (be instanceof CushionBlockEntity cushion) {
            cushion.unregisterMachine(this);
        }

        boundCushion = null;
    }

    protected CushionBlockEntity findAvailableCushion() {
        if (world == null) return null;

        for (Direction dir : Direction.Type.HORIZONTAL) {
            BlockPos checkPos = pos.offset(dir);
            BlockEntity be = world.getBlockEntity(checkPos);

            if (be instanceof CushionBlockEntity cushion) {
                if (!cushion.isFull()) {
                    return cushion;
                }
            }
        }

        return null;
    }
}
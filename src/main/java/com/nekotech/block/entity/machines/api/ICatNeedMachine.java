package com.nekotech.block.entity.machines.api;

import com.nekotech.block.entity.CushionBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public interface ICatNeedMachine {

    default World getWorld() {
        return ((BlockEntity) this).getWorld();
    }

    default BlockPos getPos() {
        return ((BlockEntity) this).getPos();
    }

    default boolean canMachineRun() {
        World world = getWorld();
        if (world == null || world.isClient) return false;

        CushionBlockEntity cushion = getBoundCushion();
        if (cushion != null) {
            return cushion.hasCatCached();
        }

        cushion = findAndBindCushion();
        return cushion != null && cushion.hasCatCached();
    }

    default CushionBlockEntity findAndBindCushion() {
        World world = getWorld();
        BlockPos pos = getPos();

        for (Direction dir : Direction.Type.HORIZONTAL) {
            BlockEntity be = world.getBlockEntity(pos.offset(dir));
            if (be instanceof CushionBlockEntity cushion && !cushion.isFull()) {
                if (cushion.tryRegister((BlockEntity) this)) {
                    setBoundCushion(cushion.getPos());
                    return cushion;
                }
            }
        }
        return null;
    }

    /* ========= 状态存取（由实现类存 NBT） ========= */

    void setBoundCushion(BlockPos pos);

    CushionBlockEntity getBoundCushion();

    default void unbindCushion() {
        CushionBlockEntity cushion = getBoundCushion();
        if (cushion != null) {
            cushion.unregisterMachine((ICatNeedMachine) this);
        }
        setBoundCushion(null);
    }
}
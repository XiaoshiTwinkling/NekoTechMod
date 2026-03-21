package com.nekotech.block.entity.machines;

import com.nekotech.NekoTechnology;
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

    // 构造函数
    public CatNeedMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public boolean hasCatOnCushion(BlockPos cushionPos) {
        if (world == null) {
            return false;
        }

        BlockPos abovePos = cushionPos.up();

        List<CatEntity> cats = world.getEntitiesByClass(
                CatEntity.class,
                new net.minecraft.util.math.Box(abovePos),
                cat -> cat != null && cat.isAlive()
        );

        return !cats.isEmpty();
    }

    //用来判断机器四周有没有坐垫，且坐垫上有猫
    public boolean hasCushionWithCatAround() {
        if (world == null) {
            return false;
        }

        Direction[] directions = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};

        for (Direction dir : directions) {
            BlockPos checkPos = pos.offset(dir);
            BlockState state = world.getBlockState(checkPos);

            if (state.getBlock() == ModBlocks.cushion_block) {
                // 检查坐垫上是否有猫
                if (hasCatOnCushion(checkPos)) {
                    return true;
                }
            }
        }

        return false;
    }
    //判断这个机器是否可以运行
    public boolean canMachineRun() {
        return hasCushionWithCatAround();
    }
}
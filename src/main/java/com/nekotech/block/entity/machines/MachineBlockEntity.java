package com.nekotech.block.entity.machines;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/*
这是模组中所有机器的基类
 */

public abstract class MachineBlockEntity extends BlockEntity {

    private static final int LAZY_TICK_INTERVAL = 200;
    private int lazyTickTimer = LAZY_TICK_INTERVAL - 1;

    // 构造函数
    public MachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // 保存数据
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup){
        super.writeNbt(nbt, registryLookup);
    };

    // 加载数据
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup){
        super.readNbt(nbt, registryLookup);
    };

    //lazytick的计时器函数
    protected void baseTick(World world, BlockPos pos, BlockState state) {
        if (world.isClient) {
            return;
        }
        lazyTickTimer++;
        if (lazyTickTimer >= LAZY_TICK_INTERVAL) {
            lazyTickTimer = 0;
            lazytick(world, pos, state);
        }
    }

    //这个函数每10s调用一次
    public abstract void lazytick(World world, BlockPos pos, BlockState state);

}
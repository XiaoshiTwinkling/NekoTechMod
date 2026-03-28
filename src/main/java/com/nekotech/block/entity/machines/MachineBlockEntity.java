package com.nekotech.block.entity.machines;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;

/*
这是模组中所有机器的基类
 */

public abstract class MachineBlockEntity extends BlockEntity {

    // 构造函数
    public MachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // 保存数据
    protected abstract void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookupt);

    // 加载数据
    public abstract void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup);
}
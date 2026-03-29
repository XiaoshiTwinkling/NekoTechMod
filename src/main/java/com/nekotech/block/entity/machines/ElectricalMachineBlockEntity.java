package com.nekotech.block.entity.machines;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;


/*
这是模组中所有用电机器的基类
 */

public class ElectricalMachineBlockEntity extends CatNeedMachineBlockEntity{

    public ElectricalMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    protected float nekoFlux = 0; //机器含有的猫猫能量

    public float getNekoFlux() {
        return nekoFlux;
    }

    public void setNekoFlux(float setNekoFlux) {
        this.nekoFlux=setNekoFlux;
        markDirty();
    }

    public void resetNekoFlux() {
        this.nekoFlux = 0.0f;
        markDirty();
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt,registryLookup);
        nbt.putFloat("NekoFlux", nekoFlux);
    }

    @Override
    public void readNbt(NbtCompound nbt,RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt,registryLookup);
        nekoFlux = nbt.getFloat("NekoFlux");
    }

    @Override
    public void lazytick(World world, BlockPos pos, BlockState state) {

    }
}

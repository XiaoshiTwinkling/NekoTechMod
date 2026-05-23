package com.nekotech.block.entity.machines.conductor;

import com.nekotech.block.entity.ModBlockEntities;
import com.nekotech.block.entity.api.electrical.ITransferElectrical;
import com.nekotech.block.entity.machines.MachineBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class MachineCasingBlockEntity extends MachineBlockEntity implements ITransferElectrical {
    public MachineCasingBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WOODEN_CASING,pos, state);
    }

    @Override
    public void lazytick(World world, BlockPos pos, BlockState state) {
    }
}

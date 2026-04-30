package com.nekotech.item.custom.component;

import com.nekotech.block.entity.machines.api.ComponentAdaptation;
import com.nekotech.block.entity.machines.api.IElectricalMachine;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

//for test
public class BrassFluxOutputItem extends AbstractComponentItem{
    public BrassFluxOutputItem() {
        super(new Item.Settings().maxCount(16));
    }

    @Override
    public void useComponent(World world, ComponentAdaptation self, Direction side) {
        BlockPos targetPos = self.getPos().offset(side);
        BlockEntity neighbor = world.getBlockEntity(targetPos);

        if (neighbor instanceof IElectricalMachine receiver) {
            receiver.receiveFlux(1); // test:每 tick 输出 1 NekoFlux
        }
    }
}

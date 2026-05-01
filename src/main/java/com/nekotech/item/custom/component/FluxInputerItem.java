package com.nekotech.item.custom.component;

import com.nekotech.block.entity.machines.api.ComponentAdaptation;
import com.nekotech.block.entity.machines.api.IElectricalMachine;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class FluxInputerItem extends AbstractComponentItem{
    final float inputSpeed;

    public FluxInputerItem(float inputSpeed, String tooltipTranslationKey) {
        super(new Settings().maxCount(16), tooltipTranslationKey);
        this.inputSpeed = inputSpeed;
    }

    @Override
    public void useComponent(World world, ComponentAdaptation self, Direction side) {
        BlockPos targetPos = self.getPos().offset(side);
        BlockEntity neighbor = world.getBlockEntity(targetPos);
        if (self instanceof IElectricalMachine adder && neighbor instanceof IElectricalMachine receiver) {
            if(adder.getNekoFlux() < adder.getMaxNekoFlux() && receiver.getNekoFlux() > 0){
                receiver.receiveFlux(inputSpeed);
                adder.addFlux(inputSpeed);
            }
        }
    }
}

package com.nekotech.item.custom.component;

import com.nekotech.block.entity.machines.api.ComponentAdaptation;
import com.nekotech.block.entity.machines.api.IElectricalMachine;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class FluxOutputerItem extends AbstractComponentItem{
    final float outputSpeed;

    public FluxOutputerItem(float outputSpeed, String tooltipTranslationKey) {
        super(new Item.Settings().maxCount(16), tooltipTranslationKey);
        this.outputSpeed = outputSpeed;
    }

    @Override
    public void useComponent(World world, ComponentAdaptation self, Direction side) {
        BlockPos targetPos = self.getPos().offset(side);
        BlockEntity neighbor = world.getBlockEntity(targetPos);
        if (neighbor instanceof IElectricalMachine adder && self instanceof IElectricalMachine receiver) {
            if(adder.getNekoFlux() < adder.getMaxNekoFlux() && receiver.getNekoFlux() > 0){
                receiver.receiveFlux(outputSpeed);
                adder.addFlux(outputSpeed);
            }
        }
    }
}

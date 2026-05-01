package com.nekotech.item.custom.component;

import com.nekotech.block.entity.api.component.ComponentAdaptation;
import com.nekotech.block.entity.api.electrical.IElectricalMachine;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * 可以把对面能量吸过来的零件喵~
 */
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

package com.nekotech.block.entity.machines;

import com.nekotech.block.entity.api.component.ComponentAdaptation;
import com.nekotech.block.entity.api.electrical.IElectricalMachine;
import com.nekotech.block.entity.api.electrical.ITransferElectrical;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * 用电器的基类 所有用电器都用这个喵~
 */
public abstract class AbstractElectricalAppliance extends MachineBlockEntity
        implements IElectricalMachine, ITransferElectrical, ComponentAdaptation {

    private float neko_flux = 0.0f;

    private final Map<Direction, Item> attachedComponents = new EnumMap<>(Direction.class);
    private final Set<Item> validComponents ;

    //猫猫通量每tick的使用速率喵
    private float flux_usage_rate = 0.0f;

    public AbstractElectricalAppliance(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.validComponents = null;
    }

    @Override
    public float getNekoFlux() {
        return neko_flux;
    }

    @Override
    public void setNekoFlux(float value) {
        this.neko_flux = value;
    }

    @Override
    public Map<Direction, Item> getAttachedComponents() {
        return attachedComponents;
    }

    @Override
    public Set<Item> getValidComponents() {
        return validComponents;
    }

    @Override
    public boolean attachComponent(Direction side, Item component) {
        if (!canAttachComponent(side, component)) {
            return false;
        }
        attachedComponents.put(side, component);

        this.markDirty();

        if (world != null && !world.isClient()) {
            world.updateListeners(pos, getCachedState(), getCachedState(), 3);
        }
        return true;
    }

    @Override
    public void removeComponent(Direction side) {
        attachedComponents.remove(side);
        this.markDirty();
    }

    // 执行所有安装的零件喵
    public void tickComponents() {
        if (world == null || world.isClient()) return;

        for (Direction side : Direction.values()) {
            Item component = getComponent(side);
            if (component != null) {
                componentTick(world, side);
            }
        }
    }

    public float getFlux_usage_rate() {
        return flux_usage_rate;
    }

    public void setFlux_usage_rate(float value) {
        this.flux_usage_rate = value;
    }

    /**
     * 这个函数用来在机器正在工作的时候 每tick减少能量
     */
    public void useFlux(){
        if(isWorking()){
            receiveFlux(flux_usage_rate);
        }
    }

    /**
     * 这个函数来得到机器是否正在工作
     */
    public abstract boolean isWorking();

}

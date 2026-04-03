package com.nekotech.block.entity.machines;

import com.nekotech.block.entity.ModBlockEntities;
import com.nekotech.item.block.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class AlloyPotBlockEntity extends TakeFreelyMachineBlockEntity{

    public AlloyPotBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.alloy_pot, pos, state, 2);
    }

    public static final int INPUT_SLOT_1 = 0;     // 输入槽1
    public static final int INPUT_SLOT_2 = 1;     // 输入槽2

    private float temperature = 0;  // 当前温度

    private int alloyProgress = 0;      // 当前合金进度
    private int alloyTimeTotal = 0;     // 总所需时间

    public static void tick(World world, BlockPos pos, BlockState state, AlloyPotBlockEntity blockEntity) {
        if (world.isClient) {
            blockEntity.clientTick();
        } else {
            blockEntity.serverTick();
        }
    }

    public void clientTick() {
        // 客户端逻辑
    }

    public void serverTick() {
        if (world.isClient) {
            return;
        }

        updateTemperature();

        if (canWork()) {

        }
    }

    private boolean canWork() {
        // 这里要获取方块位置
        BlockPos belowPos = getPos().down();
        BlockState belowState = getWorld().getBlockState(belowPos);
        return canMachineRun() && temperature >= 1 && isHeater(belowState);
    }


    private void updateTemperature() {
        // 这里要获取方块位置
        BlockPos belowPos = getPos().down();
        BlockState belowState = getWorld().getBlockState(belowPos);

        if (isHeater(belowState)) {
            temperature = getHeaterTemperature();
        } else {
            temperature = 0;
        }
    }

    private boolean isHeater(BlockState belowState) {
        return belowState.isOf(ModBlocks.heater);
    }

    public HeaterBlockEntity getHeaterBelow() {
        if (this.world == null) return null;

        BlockPos belowPos = this.pos.down();
        BlockEntity blockEntity = this.world.getBlockEntity(belowPos);

        if (blockEntity instanceof HeaterBlockEntity heater) {
            return heater;
        }
        return null;
    }

    private float getHeaterTemperature() {
        HeaterBlockEntity heater = getHeaterBelow();
        return heater != null ? heater.getTemperature() : 0f;
    }

    @Override
    public int getMaxCountPerStack() {
        return 1;
    }
}
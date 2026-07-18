package com.nekotech.block.entity.machines.conductor;

import com.nekotech.block.entity.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public class DebugFluxStorageBlockEntity extends FluxStorageBlockEntity{

    private static final float DEBUG_MAX_FLUX = 1000f;

    public DebugFluxStorageBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DEBUG_FLUX_STORAGE,pos, state);
        this.setNekoFlux(DEBUG_MAX_FLUX);
    }

    @Override
    public float getMaxNekoFlux() {
        return DEBUG_MAX_FLUX;
    }

    @Override
    public void addFlux(float addingFlux){
        this.setNekoFlux(DEBUG_MAX_FLUX);
    }

    @Override
    public void receiveFlux(float receiveFlux){
        this.setNekoFlux(DEBUG_MAX_FLUX);
    }
}

package com.nekotech.block.entity.machines;

import com.nekotech.block.entity.ModBlockEntities;
import com.nekotech.block.entity.api.ICatTaskBlockEntity;
import com.nekotech.item.custom.NekoTag.NekoTask;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

import java.util.Map;

public class AdvancedWorkBenchBlockEntity extends WorkBenchBlockEntity
        implements ICatTaskBlockEntity {

    public AdvancedWorkBenchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ADVANCED_WORK_BENCH, pos, state);
    }

    @Override
    public Map<NekoTask, NekoTaskHandler> createNekoTaskHandlers() {
        return Map.of(
                NekoTask.FORGING,
                (cat, tag) -> tryAutomatedForging()
        );
    }
}

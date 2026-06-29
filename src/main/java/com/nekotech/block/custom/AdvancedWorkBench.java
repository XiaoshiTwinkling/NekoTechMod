package com.nekotech.block.custom;

import com.mojang.serialization.MapCodec;
import com.nekotech.block.entity.machines.AdvancedWorkBenchBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class AdvancedWorkBench extends WorkBench {
    public static final MapCodec<AdvancedWorkBench> CODEC =
            createCodec(AdvancedWorkBench::new);

    public AdvancedWorkBench(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new AdvancedWorkBenchBlockEntity(pos, state);
    }
}

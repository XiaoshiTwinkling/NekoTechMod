package com.nekotech.block;

import com.mojang.serialization.MapCodec;
import com.nekotech.block.entity.api.electrical.conductor.ConductorSystem;
import com.nekotech.block.entity.machines.conductor.MachineCasingBlockEntity;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class WoodenCasing extends DirectionalMachineBlock{
    public static final MapCodec<WoodenCasing> CODEC = createCodec(WoodenCasing::new);

    public WoodenCasing(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new MachineCasingBlockEntity(pos, state);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onBlockAdded(state, world, pos, oldState, notify);
        if (!world.isClient()) {
            ConductorSystem.onBlockPlaced((ServerWorld) world, pos);
        }
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock()) && !world.isClient()) {
            ConductorSystem.onBlockBroken((ServerWorld) world, pos);
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }
}

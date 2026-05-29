package com.nekotech.block;

import com.mojang.serialization.MapCodec;
import com.nekotech.block.entity.ModBlockEntities;
import com.nekotech.block.entity.api.electrical.conductor.ConductorSystem;
import com.nekotech.block.entity.machines.conductor.FluxStorageBlockEntity;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class FluxStorage extends BlockWithEntity implements BlockEntityProvider {

    public static final MapCodec<FluxStorage> CODEC = createCodec(FluxStorage::new);

    public FluxStorage(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new FluxStorageBlockEntity(pos, state);
    }

    @Nullable
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            World world, BlockState state, BlockEntityType<T> type
    ) {
        return validateTicker(type, ModBlockEntities.FLUX_STORAGE,
                FluxStorageBlockEntity::tick);
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock()) && !world.isClient()) {
            ConductorSystem.onBlockBroken((ServerWorld) world, pos);
        }

        if (!state.isOf(newState.getBlock())) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof FluxStorageBlockEntity fluxStorage) {
                if (!world.isClient()) {
                    Map<net.minecraft.util.math.Direction, net.minecraft.item.Item> components =
                            fluxStorage.getAllComponentsForDrop();
                    for (Map.Entry<net.minecraft.util.math.Direction, net.minecraft.item.Item> entry : components.entrySet()) {
                        net.minecraft.item.Item componentItem = entry.getValue();
                        if (componentItem != null) {
                            net.minecraft.item.ItemStack stack = new net.minecraft.item.ItemStack(componentItem, 1);
                            net.minecraft.block.Block.dropStack(world, pos, stack);
                        }
                    }
                }
            }
        }

        // 调用父类方法
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onBlockAdded(state, world, pos, oldState, notify);
        if (!world.isClient()) {
            ConductorSystem.onBlockPlaced((ServerWorld) world, pos);
        }
    }
}

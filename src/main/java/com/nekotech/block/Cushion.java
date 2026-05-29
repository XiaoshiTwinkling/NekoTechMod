package com.nekotech.block;

import com.mojang.serialization.MapCodec;
import com.nekotech.block.entity.CushionBlockEntity;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class Cushion extends BlockWithEntity{

    // 碰撞箱：和下半砖相同（8像素高）
    private static final VoxelShape SHAPE = Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 8.0, 16.0);

    public Cushion(Settings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world,
                                      BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CushionBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            World world, BlockState state, BlockEntityType<T> type) {

        return world.isClient ? null : (w, pos, s, be) -> {
            if (be instanceof CushionBlockEntity cushion) {
                CushionBlockEntity.tick(w, pos, s, cushion);
            }
        };
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return null;
    }


    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }
}
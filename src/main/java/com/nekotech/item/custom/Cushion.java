package com.nekotech.item.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Cushion extends Block{

    // 碰撞箱：和下半砖相同（8像素高）
    private static final VoxelShape SHAPE = Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 8.0, 16.0);

    public Cushion(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return null;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getRaycastShape(BlockState state, BlockView world, BlockPos pos) {
        return SHAPE;
    }

    public static boolean hasCatAbove(World world, BlockPos pos) {
        if (world == null) {
            return false;
        }

        // 检查正上方一格
        BlockPos abovePos = pos.up();

        // 获取方块上方的猫
        List<CatEntity> cats = world.getEntitiesByClass(
                CatEntity.class,
                new net.minecraft.util.math.Box(abovePos),
                cat -> cat != null && cat.isAlive()
        );

        return !cats.isEmpty();
    }

    @Nullable
    public static CatEntity getCatAbove(World world, BlockPos pos) {
        if (world == null) {
            return null;
        }

        BlockPos abovePos = pos.up();
        List<CatEntity> cats = world.getEntitiesByClass(
                CatEntity.class,
                new net.minecraft.util.math.Box(abovePos),
                cat -> cat != null && cat.isAlive()
        );

        return cats.isEmpty() ? null : cats.get(0);
    }

    public static boolean hasCatAbove(World world, BlockPos pos, int rangeY) {
        if (world == null || rangeY <= 0) {
            return false;
        }

        // 创建搜索范围
        net.minecraft.util.math.Box searchBox = new net.minecraft.util.math.Box(
                pos.getX(), pos.getY() + 1, pos.getZ(),
                pos.getX() + 1, pos.getY() + 1 + rangeY, pos.getZ() + 1
        );

        List<CatEntity> cats = world.getEntitiesByClass(
                CatEntity.class,
                searchBox,
                cat -> cat != null && cat.isAlive()
        );

        return !cats.isEmpty();
    }


    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }
}
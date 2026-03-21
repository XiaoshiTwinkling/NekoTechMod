package com.nekotech.block.entity.machines;

import com.mojang.serialization.MapCodec;
import com.nekotech.NekoTechnology;
import com.nekotech.block.entity.ModBlockEntities;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CushionBlockEntity extends BlockWithEntity {

    // 碰撞箱：和下半砖相同（8像素高）
    private static final VoxelShape SHAPE = Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 8.0, 16.0);

    public CushionBlockEntity(Settings settings) {
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

    public void tick(World world, BlockPos pos, BlockState state) {
        if (world.isClient()) {
            return;
        }
        NekoTechnology.LOGGER.info("111");
        if(hasCatAbove(world, pos)){
            NekoTechnology.LOGGER.info("Suc");
        }

    }


    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return null;  // 这个方块不需要方块实体
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }
}
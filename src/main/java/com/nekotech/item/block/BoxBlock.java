package com.nekotech.item.block;

import com.mojang.serialization.MapCodec;
import com.nekotech.block.entity.BoxBlockEntity;
import com.nekotech.block.entity.ModBlockEntities;
import com.nekotech.block.entity.machines.FluxStorageBlockEntity;
import net.minecraft.block.AbstractChestBlock;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoubleBlockProperties;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Supplier;

public class BoxBlock extends AbstractChestBlock<BoxBlockEntity> {

    public static final MapCodec<BoxBlock> CODEC = createCodec(settings -> new BoxBlock(settings, () -> ModBlockEntities.basic_storage_enclosure));

    public BoxBlock(Settings settings, Supplier<BlockEntityType<? extends BoxBlockEntity>> blockEntityTypeSupplier) {
        super(settings, blockEntityTypeSupplier);
    }

    @Override
    protected MapCodec<? extends AbstractChestBlock<BoxBlockEntity>> getCodec() {
        return CODEC;
    }

    @Override
    public DoubleBlockProperties.PropertySource<? extends ChestBlockEntity> getBlockEntitySource(BlockState state, World world, BlockPos pos, boolean ignoreBlocked) {
        return null;
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new BoxBlockEntity(pos, state);
    }

    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit){
        if (!world.isClient()) {
            NamedScreenHandlerFactory factory = this.createScreenHandlerFactory(state, world, pos);
            if (factory != null) {
                player.openHandledScreen(factory);
                return ActionResult.SUCCESS;
            }
        }
        return ActionResult.CONSUME;
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        // 获取方块实体
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof BoxBlockEntity boxBlockEntity) {
            // 使用原版的方法分散物品
            ItemScatterer.spawn(world, pos, boxBlockEntity);
            // 更新比较器
            world.updateComparators(pos, this);
        }

        // 调用父类方法
        super.onBreak(world, pos, state, player);
        return state;
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof FluxStorageBlockEntity fluxStorage) {
                if (!world.isClient()) {
                    Map<Direction, Item> components =
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
}

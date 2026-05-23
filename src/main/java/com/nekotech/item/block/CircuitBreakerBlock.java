package com.nekotech.item.block;

import com.mojang.serialization.MapCodec;
import com.nekotech.block.entity.ModBlockEntities;
import com.nekotech.block.entity.api.electrical.conductor.ConductorSystem;
import com.nekotech.block.entity.machines.conductor.CircuitBreakerBlockEntity;
import com.nekotech.block.entity.machines.conductor.FluxStorageBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class CircuitBreakerBlock extends BlockWithEntity {

    public static final BooleanProperty POWERED = Properties.POWERED;

    public static final BooleanProperty OPEN = BooleanProperty.of("open");

    public static final MapCodec<CircuitBreakerBlock> CODEC = createCodec(CircuitBreakerBlock::new);

    public CircuitBreakerBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState());
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(POWERED, OPEN);
    }

    /**
     * 当方块被放置时调用喵~
     */
    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable net.minecraft.entity.LivingEntity placer, net.minecraft.item.ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);

        if (!world.isClient()) {
            // 方块放置后，通知导体系统扫描喵~
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof CircuitBreakerBlockEntity breaker) {
                breaker.updateConductorSystem();
            }
        }
    }

    /**
     * 当方块被玩家右键点击时调用喵~
     */
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos,
                              PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient() && player.getActiveHand() == Hand.MAIN_HAND) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof CircuitBreakerBlockEntity breaker) {
                // 切换手动开关
                boolean newOpenState = breaker.toggleManualSwitch();

                // 更新方块状态
                world.setBlockState(pos, state.with(OPEN, newOpenState)
                        .with(POWERED, breaker.isPoweredByRedstone()), Block.NOTIFY_ALL);

                return ActionResult.SUCCESS;
            }
        }
        return ActionResult.success(world.isClient());
    }

    /**
     * 当红石信号变化时调用喵~
     */
    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos,
                               Block sourceBlock, BlockPos sourcePos, boolean notify) {
        if (!world.isClient()) {
            // 检查红石信号
            boolean hasPower = world.isReceivingRedstonePower(pos);
            boolean wasPowered = state.get(POWERED);

            if (hasPower != wasPowered) {
                // 更新方块的红石状态
                world.setBlockState(pos, state.with(POWERED, hasPower)
                        .with(OPEN, state.get(OPEN)), Block.NOTIFY_ALL);
            }
        }
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }


    /**
     * 当方块被替换或移除时调用喵~
     * 确保导体系统知道这个方块被移除了喵~
     */
    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock()) && !world.isClient()) {
            ConductorSystem.onBlockBroken((ServerWorld) world, pos);
        }

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

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onBlockAdded(state, world, pos, oldState, notify);
        if (!world.isClient()) {
            ConductorSystem.onBlockPlaced((ServerWorld) world, pos);
        }
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CircuitBreakerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state,
                                                                  BlockEntityType<T> type) {
        return checkType(type, ModBlockEntities.CIRCUIT_BREAKER,
                CircuitBreakerBlockEntity::tick);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> checkType(
            BlockEntityType<A> givenType, BlockEntityType<E> expectedType,
            BlockEntityTicker<? super E> ticker) {
        return expectedType == givenType ? (BlockEntityTicker<A>) ticker : null;
    }
}

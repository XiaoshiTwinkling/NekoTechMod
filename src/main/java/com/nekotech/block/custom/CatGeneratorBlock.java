package com.nekotech.block.custom;

import com.mojang.serialization.MapCodec;
import com.nekotech.block.entity.ModBlockEntities;
import com.nekotech.block.entity.api.electrical.conductor.ConductorSystem;
import com.nekotech.block.entity.machines.CatGeneratorBlockEntity;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.stream.Stream;

public class CatGeneratorBlock extends BlockWithEntity {
    public static final MapCodec<CatGeneratorBlock> CODEC = createCodec(CatGeneratorBlock::new);
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public static final EnumProperty<CatGeneratorPart> PART = EnumProperty.of("part", CatGeneratorPart.class);

    private static final VoxelShape LEFT_SHAPE = VoxelShapes.union(
            Block.createCuboidShape(0, 0, 0, 16, 4, 15),
            Block.createCuboidShape(0, 4, 0, 3, 6, 15),
            Block.createCuboidShape(13, 4, 0, 16, 6, 15)
    );

    private static final VoxelShape RIGHT_SHAPE = VoxelShapes.union(
            Block.createCuboidShape(0, 0, 1, 16, 4, 16),
            Block.createCuboidShape(0, 4, 8, 16, 6, 16),
            Block.createCuboidShape(0, 4, 1, 16, 6, 8),
            Block.createCuboidShape(0, 6, 1, 16, 12, 8)
    );

    public CatGeneratorBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getStateManager().getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(PART, CatGeneratorPart.LEFT));
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return state.get(PART) == CatGeneratorPart.LEFT ? LEFT_SHAPE : RIGHT_SHAPE;
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        World world = ctx.getWorld();
        BlockPos clickedPos = ctx.getBlockPos();
        Direction facing = ctx.getHorizontalPlayerFacing().getOpposite();
        Direction rightDirection = getRightDirection(facing);

        BlockPos preferredRightPos = clickedPos.offset(rightDirection);
        if (world.getBlockState(preferredRightPos).canReplace(ctx)) {
            return this.getDefaultState()
                    .with(FACING, facing)
                    .with(PART, CatGeneratorPart.LEFT);
        }

        BlockPos fallbackLeftPos = clickedPos.offset(rightDirection.getOpposite());
        if (world.getBlockState(fallbackLeftPos).canReplace(ctx)) {
            return this.getDefaultState()
                    .with(FACING, facing)
                    .with(PART, CatGeneratorPart.RIGHT);
        }

        return null;
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        if (!world.isClient) {
            BlockPos otherPos = getOtherPartPos(pos, state);
            BlockState otherState = state.with(PART, getOppositePart(state.get(PART)));
            world.setBlockState(otherPos, otherState, Block.NOTIFY_ALL | Block.FORCE_STATE);
        }

        super.onPlaced(world, pos, state, placer, itemStack);
    }

    @Override
    protected BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
                                                   WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        Direction otherPartDirection = getDirectionToOtherPart(state.get(PART), state.get(FACING));

        if (direction == otherPartDirection) {
            boolean otherPartIsValid = neighborState.isOf(this)
                    && neighborState.get(PART) != state.get(PART)
                    && neighborState.get(FACING) == state.get(FACING);

            return otherPartIsValid ? state : Blocks.AIR.getDefaultState();
        }

        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient) {
            BlockPos otherPos = getOtherPartPos(pos, state);
            BlockState otherState = world.getBlockState(otherPos);

            if (otherState.isOf(this)
                    && otherState.get(PART) != state.get(PART)
                    && otherState.get(FACING) == state.get(FACING)) {
                world.setBlockState(otherPos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
            }
        }

        return super.onBreak(world, pos, state, player);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        BlockPos leftPos = getLeftPos(pos, state);
        BlockEntity blockEntity = world.getBlockEntity(leftPos);

        if (!(blockEntity instanceof CatGeneratorBlockEntity catGenerator)) {
            return ActionResult.PASS;
        }

        if (!world.isClient) {
            player.sendMessage(Text.translatable(
                    "block.neko-technology.cat_generator.description",
                    (int) catGenerator.getNekoFlux(),
                    (int) catGenerator.getMaxNekoFlux()
            ), true);
        }

        return ActionResult.SUCCESS;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CatGeneratorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state,
                                                                  BlockEntityType<T> type) {
        return validateTicker(type, ModBlockEntities.CAT_GENERATOR, CatGeneratorBlockEntity::tick);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            if (!world.isClient()) {
                ConductorSystem.onBlockBroken((ServerWorld) world, pos);
                BlockEntity blockEntity = world.getBlockEntity(pos);
                if (blockEntity instanceof CatGeneratorBlockEntity catGenerator) {
                    for (Map.Entry<Direction, Item> entry : catGenerator.getAllComponentsForDrop().entrySet()) {
                        Item componentItem = entry.getValue();
                        if (componentItem != null) {
                            Block.dropStack(world, pos, new ItemStack(componentItem, 1));
                        }
                    }
                }
            }
        }

        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onBlockAdded(state, world, pos, oldState, notify);
        if (!world.isClient() && !oldState.isOf(this)) {
            ConductorSystem.onBlockPlaced((ServerWorld) world, pos);
        }
    }

    public static Direction getDirectionToOtherPart(CatGeneratorPart part, Direction facing) {
        Direction rightDirection = getRightDirection(facing);
        return part == CatGeneratorPart.LEFT ? rightDirection : rightDirection.getOpposite();
    }

    public static CatGeneratorPart getOppositePart(CatGeneratorPart part) {
        return part == CatGeneratorPart.LEFT ? CatGeneratorPart.RIGHT : CatGeneratorPart.LEFT;
    }

    public static BlockPos getOtherPartPos(BlockPos pos, BlockState state) {
        return pos.offset(getDirectionToOtherPart(state.get(PART), state.get(FACING)));
    }

    public static Direction getRightDirection(Direction facing) {
        return facing.rotateYClockwise();
    }

    public static BlockPos getLeftPos(BlockPos pos, BlockState state) {
        return state.get(PART) == CatGeneratorPart.LEFT
                ? pos
                : pos.offset(getRightDirection(state.get(FACING)).getOpposite());
    }

    public static BlockPos getMainPos(BlockPos pos, BlockState state) {
        return getLeftPos(pos, state);
    }
}

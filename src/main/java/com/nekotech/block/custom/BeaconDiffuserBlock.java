package com.nekotech.block.custom;

import com.mojang.serialization.MapCodec;
import com.nekotech.block.entity.ModBlockEntities;
import com.nekotech.block.entity.machines.BeaconDiffuserBlockEntity;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

public class BeaconDiffuserBlock extends BlockWithEntity {
    public static final MapCodec<BeaconDiffuserBlock> CODEC = createCodec(BeaconDiffuserBlock::new);
    public static final BooleanProperty ACTIVE = BooleanProperty.of("active");

    private static final VoxelShape SHAPE = Block.createCuboidShape(0, 0, 0, 16, 15, 16);

    public BeaconDiffuserBlock(Settings settings) {
        super(settings.nonOpaque().solidBlock((state, world, pos) -> false));
        this.setDefaultState(this.getStateManager().getDefaultState().with(ACTIVE, false));
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
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
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL; // 使用自定义模型或默认方块模型
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new BeaconDiffuserBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return validateTicker(type, ModBlockEntities.BEACON_DIFFUSER, BeaconDiffuserBlockEntity::tick);
    }

    // 放置时检查下方是否是信标
    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        if (!world.isClient) {
            checkBeaconBelow(world, pos, state);
        }
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (!world.isClient() && direction == Direction.DOWN) {
            if (world.getBlockEntity(pos) instanceof BeaconDiffuserBlockEntity be) {
                be.updateBeaconStatus();
            }
        }
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    private void checkBeaconBelow(World world, BlockPos pos, BlockState state) {
        if (world.getBlockEntity(pos) instanceof BeaconDiffuserBlockEntity be) {
            be.updateBeaconStatus();
        }
    }
}

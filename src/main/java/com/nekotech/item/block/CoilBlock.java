package com.nekotech.item.block;

import com.mojang.serialization.MapCodec;
import com.nekotech.block.entity.ModBlockEntities;
import com.nekotech.block.entity.api.electrical.conductor.ConductorSystem;
import com.nekotech.block.entity.machines.coil.CoilBlockEntity;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class CoilBlock extends DirectionalMachineBlock {

    private static final VoxelShape SHAPE = VoxelShapes.union(
            Block.createCuboidShape(6, 0, 6, 10, 16, 10)
    );

    public static final IntProperty LAYERS = IntProperty.of("layers", 0, 6);

    public static final MapCodec<CoilBlock> CODEC = createCodec(CoilBlock::new);


    public CoilBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(LAYERS, 0));
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CoilBlockEntity(pos, state);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {

        int layers = state.get(LAYERS);

        float coreRadius = 2.0f;

        float currentRadius = coreRadius + layers;

        float min = 8.0f - currentRadius;
        float max = 8.0f + currentRadius;

        if(layers == 6){
            return Block.createCuboidShape(min+1, 0, min+1, max-1, 16, max-1);
        }

        return Block.createCuboidShape(min, 0, min, max, 16, max);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Nullable
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            World world, BlockState state, BlockEntityType<T> type
    ) {
        return validateTicker(type, ModBlockEntities.COIL_BLOCK,
                CoilBlockEntity::tick);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING).add(LAYERS);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState()
                .with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);

        if (!world.isClient) {
            ConductorSystem.onBlockPlaced((ServerWorld) world, pos);
        }
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos,
                              PlayerEntity player, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;

        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof CoilBlockEntity coil) {
            return coil.handleRightClick(player, player.getStackInHand(player.getActiveHand()))
                    ? ActionResult.SUCCESS : ActionResult.FAIL;
        }

        return ActionResult.PASS;
    }
}

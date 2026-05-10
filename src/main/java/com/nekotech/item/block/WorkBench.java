package com.nekotech.item.block;

import com.mojang.serialization.MapCodec;
import com.nekotech.block.entity.machines.AlloyPotBlockEntity;
import com.nekotech.block.entity.machines.WorkBenchBlockEntity;
import com.nekotech.item.ModItems;
import com.nekotech.item.custom.Hammer;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class WorkBench extends DirectionalMachineBlock {

    private static final VoxelShape SHAPE = Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 12.0, 16.0);

    public static final MapCodec<WorkBench> CODEC = createCodec(WorkBench::new);

    public static final BooleanProperty HAS_GLASS_COVER = BooleanProperty.of("has_glass_cover");

    protected WorkBench(Settings settings) {
        super(settings);
        this.setDefaultState(this.getStateManager().getDefaultState()
                .with(HAS_GLASS_COVER, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, HAS_GLASS_COVER);
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new WorkBenchBlockEntity(pos, state);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos,
                              PlayerEntity player, BlockHitResult hit) {
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }

        ItemStack handStack = player.getStackInHand(player.getActiveHand());
        boolean isSneaking = player.isSneaking();

        if (isSneaking) {
            boolean hasCover = state.get(HAS_GLASS_COVER);

            if (!hasCover && handStack.getItem() == ModItems.glass_cover ) {

                world.setBlockState(pos, state.with(HAS_GLASS_COVER, true), Block.NOTIFY_ALL);

                if (!player.getAbilities().creativeMode) {
                    handStack.decrement(1);
                }

                world.playSound(null, pos, SoundEvents.BLOCK_GLASS_PLACE,
                        SoundCategory.BLOCKS, 0.8f, 1.0f);

                return ActionResult.SUCCESS;
            }
            if (hasCover && (handStack.isEmpty() || player.getAbilities().creativeMode)) {
                world.setBlockState(pos, state.with(HAS_GLASS_COVER, false), Block.NOTIFY_ALL);

                if (!player.getAbilities().creativeMode) {
                    ItemStack coverStack = new ItemStack(ModItems.glass_cover);
                    if (!player.getInventory().insertStack(coverStack)) {
                        // 背包满则掉落
                        player.dropItem(coverStack, false);
                    }
                }

                world.playSound(null, pos, SoundEvents.BLOCK_GLASS_BREAK,
                        SoundCategory.BLOCKS, 0.8f, 1.0f);

                return ActionResult.SUCCESS;
            }
        }

        if (handStack.getItem() instanceof Hammer) {
            if (world.getBlockEntity(pos) instanceof WorkBenchBlockEntity workBench) {
                if (workBench.tryForging(player, handStack)) {
                    world.playSound(null, pos, SoundEvents.BLOCK_ANVIL_USE,
                            SoundCategory.BLOCKS, 0.8f, 0.8f + world.random.nextFloat() * 0.4f);
                    return ActionResult.SUCCESS;
                } else {
                    world.playSound(null, pos, SoundEvents.BLOCK_ANVIL_LAND,
                            SoundCategory.BLOCKS, 0.5f, 0.5f);
                    return ActionResult.FAIL;
                }
            }
        }

        boolean hasCover = state.get(HAS_GLASS_COVER);
        if (hasCover) {
            if (player instanceof ServerPlayerEntity) {
                player.sendMessage(Text.translatable("message.neko-technology.workbench.covered"), true);
            }
            return ActionResult.FAIL;
        }
        if (world.getBlockEntity(pos) instanceof WorkBenchBlockEntity workBench) {
            return workBench.handleRightClick(player, handStack) ? ActionResult.SUCCESS : ActionResult.PASS;
        }

        return ActionResult.PASS;
    }

    @Override
    protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof WorkBenchBlockEntity be) {
                ItemScatterer.spawn(world, pos, be);
                world.updateComparators(pos, this);
            }

            super.onStateReplaced(state, world, pos, newState, moved);
        }
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world,
                                      BlockPos pos, ShapeContext context) {
        return SHAPE;
    }
}

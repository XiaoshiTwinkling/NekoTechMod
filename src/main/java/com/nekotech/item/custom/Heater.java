package com.nekotech.item.custom;

import com.mojang.serialization.MapCodec;
import com.nekotech.block.entity.ModBlockEntities;
import com.nekotech.block.entity.machines.HeaterBlockEntity;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class Heater extends BlockWithEntity {
    public static final BooleanProperty LIT = Properties.LIT;

    public static final MapCodec<Heater> CODEC = createCodec(Heater::new);

    public Heater(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(LIT, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState();
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }

        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof HeaterBlockEntity heater) {
            Hand hand = player.getActiveHand();
            ItemStack held = player.getStackInHand(hand);

            if (!held.isEmpty()) {
                if (heater.addFuel(held)) {
                    if (!player.isCreative()) {
                        held.decrement(1);
                    }
                    world.playSound(null, pos, SoundEvents.BLOCK_CAMPFIRE_CRACKLE,
                            SoundCategory.BLOCKS, 0.5f, 1.0f);

                    return ActionResult.CONSUME;
                }
            }
        }

        return ActionResult.PASS;
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof HeaterBlockEntity heater) {
                ItemScatterer.spawn(world, pos, heater.getItems());
            }
            super.onStateReplaced(state, world, pos, newState, moved);
        }
    }


    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new HeaterBlockEntity(pos, state);
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            World world, BlockState state, BlockEntityType<T> type) {

        return (world1, pos, state1, blockEntity) -> {
            HeaterBlockEntity.tick(world1, pos, state1, (HeaterBlockEntity) blockEntity);
        };
    }
}
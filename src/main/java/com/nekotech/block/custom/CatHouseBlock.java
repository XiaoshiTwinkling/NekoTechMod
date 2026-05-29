package com.nekotech.block.custom;

import com.mojang.serialization.MapCodec;
import com.nekotech.block.entity.CatHouseBlockEntity;
import com.nekotech.item.ModItems;
import com.nekotech.item.custom.CatBoxItem;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class CatHouseBlock extends BlockWithEntity {



    public CatHouseBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return null;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CatHouseBlockEntity(pos, state);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }

        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (!(blockEntity instanceof CatHouseBlockEntity catHouse)) {
            return ActionResult.PASS;
        }
        Hand hand= player.getActiveHand();

        ItemStack stack = player.getStackInHand(hand);
        boolean success = catHouse.interact(player, stack, hand);

        if (success) {
            return ActionResult.SUCCESS;
        }

        return super.onUse(state, world, pos, player, hit);
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof CatHouseBlockEntity catHouse && catHouse.hasCatStored()) {
                // 猫舍被破坏时，如果有猫，掉落一个装有猫的纸箱
                ItemStack catBox = new ItemStack(ModItems.NEKO_BOX, 1);
                CatBoxItem.saveCatData(catBox, catHouse.getStoredCatData());
                Block.dropStack(world, pos, catBox);
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }
}

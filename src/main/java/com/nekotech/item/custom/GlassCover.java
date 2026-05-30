package com.nekotech.item.custom;

import com.nekotech.item.ModItem;
import com.nekotech.block.custom.WorkBench;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class GlassCover extends ModItem {
    public GlassCover(Settings settings, String tooltipTranslationKey) {
        super(settings, tooltipTranslationKey);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        BlockState state = world.getBlockState(pos);

        if (!(state.getBlock() instanceof WorkBench)) {
            return ActionResult.PASS;
        }

        boolean hasCover = state.get(WorkBench.HAS_GLASS_COVER);
        var player = context.getPlayer();
        if (player == null) return ActionResult.PASS;

        boolean isSneaking = player.isSneaking();
        var handStack = context.getStack();

        if (!hasCover && isSneaking) {
            if (world.isClient) return ActionResult.SUCCESS;

            world.setBlockState(pos,
                    state.with(WorkBench.HAS_GLASS_COVER, true),
                    Block.NOTIFY_ALL);

            if (state.getBlock() instanceof WorkBench bl) {
                bl.setSHAPE(Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 16.0, 16.0));
                world.updateNeighbors(pos, bl);
            }

            if (!player.getAbilities().creativeMode) {
                handStack.decrement(1);
            }

            world.playSound(null, pos, SoundEvents.BLOCK_GLASS_PLACE,
                    SoundCategory.BLOCKS, 0.8f, 1.0f);

            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }
}

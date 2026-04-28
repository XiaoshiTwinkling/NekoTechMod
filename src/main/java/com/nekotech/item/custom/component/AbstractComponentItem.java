package com.nekotech.item.custom.component;

import com.nekotech.block.entity.machines.api.ComponentAdaptation;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public abstract class AbstractComponentItem extends Item {
    public AbstractComponentItem(Settings settings) {
        super(settings);
    }

    /**
     * 当玩家对着方块使用零件时的逻辑
     */
    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        Direction side = context.getSide();
        PlayerEntity player = context.getPlayer();

        if (!(world.getBlockEntity(pos) instanceof ComponentAdaptation machine)) {
            return ActionResult.PASS;
        }

        if (!machine.canAttachComponent(side, this)) {
            if (!world.isClient() && player != null) {
                player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value());
            }
            return ActionResult.FAIL;
        }

        if (!world.isClient()) {
            machine.attachComponent(side, this);
            context.getStack().decrement(1);
            world.playSound(null, pos, SoundEvents.BLOCK_COPPER_PLACE, SoundCategory.BLOCKS, 1f, 1f);
        }

        return ActionResult.success(world.isClient());
    }

    /**
     * 这个零件每 tick 执行的操作
     *
     * @param world   世界
     * @param self    当前零件所在的机器
     * @param side    零件所在的面
     */
    public abstract void useComponent(
            World world,
            ComponentAdaptation self,
            Direction side
    );
}

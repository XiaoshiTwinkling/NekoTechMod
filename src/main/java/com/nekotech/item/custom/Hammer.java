package com.nekotech.item.custom;

import com.nekotech.block.entity.machines.api.ComponentAdaptation;
import com.nekotech.item.AbstractDurabilityItem;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class Hammer extends Item implements AbstractDurabilityItem {
    public Hammer(Item.Settings settings) {
        super(settings.maxDamage(128));
    }

    /**
     * 当玩家用锤子右击方块时调用。
     * 新增功能：Shift + 右击已安装零件的面，将其卸下。
     */
    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        Direction side = context.getSide();
        PlayerEntity player = context.getPlayer();
        ItemStack hammerStack = context.getStack();

        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (!(blockEntity instanceof ComponentAdaptation machine)) {
            return ActionResult.PASS;
        }

        if (player != null && player.isSneaking()) {
            Item installedComponent = machine.getComponent(side);
            if (installedComponent != null) {
                if (!world.isClient()) {
                    machine.removeComponent(side);

                    (blockEntity).markDirty();
                    world.updateListeners(pos, world.getBlockState(pos), world.getBlockState(pos), 3);

                    ItemStack componentStack = new ItemStack(installedComponent, 1);
                    net.minecraft.block.Block.dropStack(world, pos, componentStack);

                    EquipmentSlot slot = player.getActiveHand() == Hand.MAIN_HAND
                            ? EquipmentSlot.MAINHAND
                            : EquipmentSlot.OFFHAND;

                    hammerStack.damage(1, player, slot);

                    world.playSound(null, pos, SoundEvents.BLOCK_COPPER_BREAK, SoundCategory.BLOCKS, 1.0f, 1.0f);
                }
                return ActionResult.success(world.isClient());
            }
        }

        return ActionResult.PASS;
    }
}

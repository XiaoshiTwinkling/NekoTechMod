package com.nekotech.item.custom;

import com.nekotech.block.entity.api.component.ComponentAdaptation;
import com.nekotech.block.entity.api.electrical.conductor.ConductorSystem;
import com.nekotech.item.api.AbstractDurabilityItem;
import com.nekotech.item.custom.component.WirePoleItem;
import com.nekotech.util.WirePairHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class Hammer extends Item implements AbstractDurabilityItem {
    public Hammer(Item.Settings settings) {
        super(settings.maxDamage(128));
    }
    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        Direction side = context.getSide();
        PlayerEntity player = context.getPlayer();

        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }

        if (player == null) {
            return ActionResult.FAIL;
        }

        // 获取目标方块实体
        var blockEntity = world.getBlockEntity(pos);
        if (!(blockEntity instanceof ComponentAdaptation machine)) {
            return ActionResult.PASS;
        }

        if (player.isSneaking()) {
            // 拆卸零件
            Item installedComponent = machine.getComponent(side);
            if (installedComponent == null) {
                return ActionResult.FAIL;
            }

            // 如果是接线柱，检查是否有配对
            if (installedComponent instanceof WirePoleItem) {
                var removedPairs = WirePairHelper.removePairAt(world, pos, side);
                if (!removedPairs.isEmpty()) {
                    WirePairHelper.giveWireBundles(player, removedPairs);
                    player.sendMessage(net.minecraft.text.Text.translatable("wire.pair.removed"), false);
                }
            }

            // 移除零件
            machine.removeComponent(side);

            if(!world.isClient()){
                ConductorSystem.onComponentChanged((ServerWorld) world , pos , side);
            }


            // 给予零件物品
            if (!player.getAbilities().creativeMode) {
                ItemStack componentStack = new ItemStack(installedComponent, 1);
                player.giveItemStack(componentStack);
            }

            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }
}

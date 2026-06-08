package com.nekotech.item.custom;

import com.nekotech.block.entity.api.component.ComponentAdaptation;
import com.nekotech.block.entity.api.electrical.conductor.ConductorSystem;
import com.nekotech.data.worlddata.ConductorWorldState;
import com.nekotech.item.AbstractDurabilityItem;
import com.nekotech.item.ModItems;
import com.nekotech.item.custom.component.WirePoleItem;
import com.nekotech.network.payload.s2c.SyncWirePairsPayload;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.Objects;

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
                WirePoleItem.PairInfo pair = WirePoleItem.getPairInfo(world, pos, side);
                if (pair != null) {
                    // 有配对，先取消配对
                    removeWirePolePair(world, pos, side, pair);

                    ConductorWorldState state = ConductorWorldState.get(Objects.requireNonNull(world.getServer()));
                    state.removeWirePairsInvolving(pos);

                    SyncWirePairsPayload.WirePairSyncHelper.syncAllPairsToPlayers(Objects.requireNonNull(world.getServer()));

                    // 返还对应的金属丝捆
                    ItemStack bundleStack = getWireBundleByType(pair.wireType);
                    if (bundleStack != null && !player.getAbilities().creativeMode) {
                        player.giveItemStack(bundleStack);
                    }

                    player.sendMessage(net.minecraft.text.Text.translatable("wire.pair.removed"), false);
                }
            }

            // 移除零件
            machine.removeComponent(side);

            // 给予零件物品
            if (!player.getAbilities().creativeMode) {
                ItemStack componentStack = new ItemStack(installedComponent, 1);
                player.giveItemStack(componentStack);
            }

            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }

    /**
     * 取消接线柱配对
     */
    private void removeWirePolePair(World world, BlockPos pos, Direction side, WirePoleItem.PairInfo pair) {
        // 删除本端的配对
        removePairData(world, pos, side);
        // 删除对端的配对
        removePairData(world, pair.targetPos, pair.targetSide);

        // 通知导体系统状态变化
        ConductorSystem.onBlockEntityStateChange((net.minecraft.server.world.ServerWorld) world, pos);
        ConductorSystem.onBlockEntityStateChange((net.minecraft.server.world.ServerWorld) world, pair.targetPos);
    }

    /**
     * 删除配对数据
     */
    private void removePairData(World world, BlockPos pos, Direction side) {
        BlockEntity be = world.getBlockEntity(pos);
        if (be == null) return;

        NbtCompound nbt = be.createNbtWithId(world.getRegistryManager());
        String key = String.format("WirePair_%s", side.getName());

        if (nbt.contains(key)) {
            nbt.remove(key);
            be.markDirty();

            if (!world.isClient) {
                world.updateListeners(pos, be.getCachedState(), be.getCachedState(), 3);
            }
        }
    }

    /**
     * 根据电线类型获取对应的金属丝捆
     */
    private ItemStack getWireBundleByType(String wireType) {
        return switch (wireType) {
            case "copper" -> new ItemStack(ModItems.COPPER_WIRE_BUNDLE, 1);
            case "brass" -> new ItemStack(ModItems.BRASS_WIRE_BUNDLE, 1);
            case "neko_copper" -> new ItemStack(ModItems.NEKO_COPPER_WIRE_BUNDLE, 1);
            default -> null;
        };
    }
}

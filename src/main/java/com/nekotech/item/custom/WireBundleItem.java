package com.nekotech.item.custom;

import com.nekotech.NekoTechnology;
import com.nekotech.block.entity.api.component.ComponentAdaptation;
import com.nekotech.block.entity.api.electrical.conductor.ConductorManager;
import com.nekotech.data.worlddata.ConductorWorldState;
import com.nekotech.item.ModItem;
import com.nekotech.item.custom.component.WirePoleItem;
import com.nekotech.network.payload.s2c.SyncWirePairsPayload;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 金属丝捆物品
 * 用于连接两个接线柱
 * 有多种类型，每种有不同的最大距离
 */
public class WireBundleItem extends ModItem {
    private final float maxDistance;
    private final String wireType;

    // 存储第一次点击信息
    private static final Map<UUID, FirstClick> firstClicks = new HashMap<>();

    private static class FirstClick {
        final BlockPos pos;
        final Direction side;
        final WireBundleItem wireBundle;

        FirstClick(BlockPos pos, Direction side, WireBundleItem wireBundle) {
            this.pos = pos;
            this.side = side;
            this.wireBundle = wireBundle;
        }
    }

    public WireBundleItem(float maxDistance, String wireType) {
        super(new Settings().maxCount(16),
                String.format("item.neko-technology.%s_wire_bundle.tooltip", wireType));
        this.maxDistance = maxDistance;
        this.wireType = wireType;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);

        if (player.isSneaking()) {
            if (!world.isClient) {
                player.sendMessage(net.minecraft.text.Text.translatable("wire.bundle.inspect",
                        String.format("%.1f", maxDistance)), false);
            }
            return TypedActionResult.success(stack, world.isClient());
        }

        return TypedActionResult.pass(stack);
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

        // 检查目标是否是接线柱
        var component = machine.getComponent(side);
        if (!(component instanceof WirePoleItem)) {
            player.sendMessage(net.minecraft.text.Text.translatable("wire.bundle.not_wirepole"), true);
            return ActionResult.FAIL;
        }

        UUID playerId = player.getUuid();
        FirstClick firstClick = firstClicks.get(playerId);

        if (firstClick == null) {
            // 第一次点击，记录位置
            firstClicks.put(playerId, new FirstClick(pos, side, this));
            player.sendMessage(net.minecraft.text.Text.translatable("wire.pair.first"), false);
            return ActionResult.SUCCESS;
        }

        // 检查是否点击了同一个接线柱
        if (pos.equals(firstClick.pos) && side == firstClick.side) {
            player.sendMessage(net.minecraft.text.Text.translatable("wire.pair.same"), false);
            firstClicks.remove(playerId);
            return ActionResult.FAIL;
        }

        // 检查距离
        double distance = getDistance(firstClick.pos, pos);
        if (distance > firstClick.wireBundle.maxDistance) {
            player.sendMessage(net.minecraft.text.Text.translatable("wire.pair.too_far",
                    String.format("%.1f", distance), firstClick.wireBundle.maxDistance), false);
            firstClicks.remove(playerId);
            return ActionResult.FAIL;
        }

        // 检查两个目标是否都是接线柱
        if (!(machine.getComponent(side) instanceof WirePoleItem)) {
            player.sendMessage(net.minecraft.text.Text.translatable("wire.pair.not_pole"), false);
            firstClicks.remove(playerId);
            return ActionResult.FAIL;
        }

        if (!canPair(world, firstClick.pos, firstClick.side, pos, side)) {
            player.sendMessage(net.minecraft.text.Text.translatable("wire.pair.already_paired"), false);
            firstClicks.remove(playerId);
            return ActionResult.FAIL;
        }

        // 创建配对
        createPair(world, firstClick.pos, firstClick.side, pos, side, firstClick.wireBundle.wireType);

        player.sendMessage(net.minecraft.text.Text.translatable("wire.pair.success"), false);
        firstClicks.remove(playerId);

        // 消耗物品
        if (!player.getAbilities().creativeMode) {
            context.getStack().decrement(1);
        }

        return ActionResult.SUCCESS;
    }

    private boolean canPair(World world, BlockPos pos1, Direction side1, BlockPos pos2, Direction side2) {
        // 使用静态方法检查配对状态
        if (WirePoleItem.hasPair(world, pos1, side1)) {
            return false;
        }

        if (WirePoleItem.hasPair(world, pos2, side2)) {
            return false;
        }

        // 检查是否已经是双向配对
        WirePoleItem.PairInfo existingPair1 = WirePoleItem.getPairInfo(world, pos1, side1);
        WirePoleItem.PairInfo existingPair2 = WirePoleItem.getPairInfo(world, pos2, side2);

        if (existingPair1 != null && existingPair1.targetPos.equals(pos2)) {
            return false;
        }

        if (existingPair2 != null && existingPair2.targetPos.equals(pos1)) {
            return false;
        }

        return true;
    }

    /**
     * 创建配对
     */
    private void createPair(World world, BlockPos pos1, Direction side1,
                            BlockPos pos2, Direction side2, String wireType) {
        if (world.isClient) return;

        ConductorWorldState state = getWorldState(world);
        if (state == null) {
            NekoTechnology.LOGGER.error("[WireBundleItem] 无法获取世界状态");
            return;
        }

        String key1 = ConductorWorldState.generateWirePairKey(pos1, side1);
        String key2 = ConductorWorldState.generateWirePairKey(pos2, side2);

        ConductorWorldState.WirePairData data1 = new ConductorWorldState.WirePairData(pos1, side1, pos2, side2, wireType);
        ConductorWorldState.WirePairData data2 = new ConductorWorldState.WirePairData(pos2, side2, pos1, side1, wireType);
        state.addWirePair(key1, data1);
        state.addWirePair(key2, data2);

        NekoTechnology.LOGGER.info("[接线柱] 创建配对: {}:{} <-> {}:{} 类型:{}",
                pos1, side1, pos2, side2, wireType);

        // 立即触发导体组重新发现
        ConductorManager manager = ConductorManager.get(world);
        if (manager != null) {
            manager.onComponentChanged(world, pos1, side1);
            manager.onComponentChanged(world, pos2, side2);
        }

        if (world instanceof ServerWorld serverWorld) {
            SyncWirePairsPayload.WirePairSyncHelper.syncAllPairsToPlayers(serverWorld.getServer());
        }
    }

    private ConductorWorldState getWorldState(World world) {
        if (world instanceof ServerWorld serverWorld) {
            return ConductorWorldState.get(serverWorld.getServer());
        }
        return null;
    }

    /**
     * 保存配对数据到方块实体
     */
    private void savePairData(World world, BlockPos pos, Direction side,
                              BlockPos targetPos, Direction targetSide, String wireType) {
        BlockEntity be = world.getBlockEntity(pos);
        if (be == null) return;

        NbtCompound nbt = be.createNbtWithId(world.getRegistryManager());
        String key = String.format("WirePair_%s", side.getName());

        NbtCompound pairNbt = new NbtCompound();
        pairNbt.putInt("TargetX", targetPos.getX());
        pairNbt.putInt("TargetY", targetPos.getY());
        pairNbt.putInt("TargetZ", targetPos.getZ());
        pairNbt.putString("TargetSide", targetSide.getName());
        pairNbt.putString("WireType", wireType);

        nbt.put(key, pairNbt);
        be.markDirty();

        if (!world.isClient) {
            world.updateListeners(pos, be.getCachedState(), be.getCachedState(), 3);
        }
    }

    /**
     * 计算两个位置之间的距离
     */
    private double getDistance(BlockPos pos1, BlockPos pos2) {
        return Math.sqrt(pos1.getSquaredDistance(pos2));
    }

    /**
     * 获取最大连接距离
     */
    public float getMaxDistance() {
        return maxDistance;
    }

    /**
     * 获取电线类型
     */
    public String getWireType() {
        return wireType;
    }

    /**
     * 获取物品名称翻译键
     */
    public String getTranslationKey() {
        return String.format("item.neko-technology.%s_wire_bundle", wireType);
    }
}
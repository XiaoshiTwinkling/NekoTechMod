package com.nekotech.item.custom.component;

import com.nekotech.block.entity.api.component.ComponentAdaptation;
import com.nekotech.block.entity.api.electrical.ITransferElectrical;
import com.nekotech.data.worlddata.ConductorWorldState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * 接线柱零件
 * 安装在实现了ComponentAdaptation的方块上
 * 用于与其他接线柱配对，建立虚拟导体连接
 */
public class WirePoleItem extends AbstractComponentItem {

    public WirePoleItem() {
        super(new Settings().maxCount(16), "item.neko-technology.wire_pole.tooltip");
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);

        if (player.isSneaking()) {
            if (!world.isClient) {
                player.sendMessage(net.minecraft.text.Text.translatable("wire.pole.inspect"), false);
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
            player.sendMessage(net.minecraft.text.Text.translatable("wire.pole.no_machine"), true);
            return ActionResult.FAIL;
        }

        // 检查是否可以安装接线柱
        if (!machine.canAttachComponent(side, this)) {
            player.sendMessage(net.minecraft.text.Text.translatable("wire.pole.cannot_attach"), true);
            return ActionResult.FAIL;
        }

        // 检查是否已安装了零件
        var existingComponent = machine.getComponent(side);
        if (existingComponent != null) {
            player.sendMessage(net.minecraft.text.Text.translatable("wire.pole.already_attached"), true);
            return ActionResult.FAIL;
        }

        // 安装接线柱
        boolean success = machine.attachComponent(side, this);
        if (!success) {
            player.sendMessage(net.minecraft.text.Text.translatable("wire.pole.attach_failed"), true);
            return ActionResult.FAIL;
        }

        // 消耗物品（创造模式不消耗）
        if (!player.getAbilities().creativeMode) {
            context.getStack().decrement(1);
        }

        // 播放安装音效
        world.playSound(
                null,
                pos,
                net.minecraft.sound.SoundEvents.BLOCK_COPPER_PLACE,
                net.minecraft.sound.SoundCategory.BLOCKS,
                0.5f,
                1.0f
        );

        // 发送成功消息
        player.sendMessage(net.minecraft.text.Text.translatable("wire.pole.attached",
                side.getName().toUpperCase()), false);

        return ActionResult.SUCCESS;
    }

    @Override
    public void useComponent(World world, ComponentAdaptation self, Direction side) {
        // 接线柱是静态连接，不需要每 tick 检查
        // 这个方法是 AbstractComponentItem 要求的，但接线柱不需要执行每 tick 操作
    }

    /**
     * 检查是否有配对
     */
    public static boolean hasPair(World world, BlockPos pos, Direction side) {
        if (world.isClient) return false;
        ConductorWorldState state = getWorldState(world);
        if (state == null) return false;
        String key = ConductorWorldState.generateWirePairKey(pos, side);
        return state.getWirePairs().containsKey(key);
    }


    /**
     * 获取配对信息
     */
    public static PairInfo getPairInfo(World world, BlockPos pos, Direction side) {
        if (world.isClient) return null;
        ConductorWorldState state = getWorldState(world);
        if (state == null) return null;
        String key = ConductorWorldState.generateWirePairKey(pos, side);
        ConductorWorldState.WirePairData data = state.getWirePairs().get(key);
        if (data == null) return null;
        return new PairInfo(data.pos2, data.side2, data.wireType);
    }

    public static ConductorWorldState getWorldState(World world) {
        if (world instanceof ServerWorld serverWorld) {
            return ConductorWorldState.get(serverWorld.getServer());
        }
        return null;
    }

    /**
     * 检查两个位置之间是否有配对
     */
    public static boolean isPaired(World world, BlockPos pos1, BlockPos pos2) {
        // 检查所有方向
        for (Direction dir : Direction.values()) {
            PairInfo pair = getPairInfo(world, pos1, dir);
            if (pair != null && pair.targetPos.equals(pos2)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 配对信息类
     */
    public static class PairInfo {
        public final BlockPos targetPos;
        public final Direction targetSide;
        public final String wireType;

        public PairInfo(BlockPos targetPos, Direction targetSide, String wireType) {
            this.targetPos = targetPos;
            this.targetSide = targetSide;
            this.wireType = wireType;
        }
    }
}
package com.nekotech.item.custom;

import com.nekotech.block.entity.ElevatorCoreBlockEntity;
import com.nekotech.block.custom.elevator.ElevatorPartBlock;
import com.nekotech.block.custom.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ElevatorItem extends Item {
    private static final Map<UUID, PendingPlacement> PENDING = new HashMap<>();

    private record PendingPlacement(RegistryKey<World> worldKey, BlockPos pos) {
    }

    public ElevatorItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        PlayerEntity player = context.getPlayer();

        if (player == null) {
            return ActionResult.FAIL;
        }

        if (world.isClient) {
            return ActionResult.SUCCESS;
        }

        if (!player.getAbilities().allowModifyWorld) {
            return ActionResult.FAIL;
        }

        ItemPlacementContext placementContext = new ItemPlacementContext(context);
        BlockPos targetPos = placementContext.getBlockPos();

        UUID uuid = player.getUuid();
        PendingPlacement pending = PENDING.remove(uuid);

        if (pending == null) {
            PENDING.put(uuid, new PendingPlacement(world.getRegistryKey(), targetPos.toImmutable()));
            player.sendMessage(Text.translatable("message.neko-technology.first_pos"), true);
            return ActionResult.SUCCESS;
        }

        if (!pending.worldKey().equals(world.getRegistryKey())) {
            player.sendMessage(Text.translatable("message.neko-technology.invalid"), true);
            return ActionResult.SUCCESS;
        }

        BlockPos first = pending.pos();
        BlockPos second = targetPos;

        if (first.getX() != second.getX() || first.getZ() != second.getZ()) {
            player.sendMessage(Text.translatable("message.neko-technology.not_vertical"), true);
            return ActionResult.SUCCESS;
        }

        int distance = Math.abs(first.getY() - second.getY());

        if (distance == 0 || distance > ElevatorCoreBlockEntity.MAX_VERTICAL_DISTANCE) {
            player.sendMessage(Text.translatable("message.neko-technology.too_far"), true);
            return ActionResult.SUCCESS;
        }

        int bottomY = Math.min(first.getY(), second.getY());
        int topY = Math.max(first.getY(), second.getY());
        BlockPos bottom = new BlockPos(first.getX(), bottomY, first.getZ());
        int height = topY - bottomY + 1;

        if (!canPlaceWholeStructure(world, bottom, height)) {
            player.sendMessage(Text.translatable("message.neko-technology.blocked"), true);
            return ActionResult.SUCCESS;
        }

        placeStructure(world, bottom, height);

        if (!player.getAbilities().creativeMode) {
            context.getStack().decrement(1);
        }

        player.sendMessage(Text.translatable("message.neko-technology.placed"), true);

        return ActionResult.SUCCESS;
    }

    private static boolean canPlaceWholeStructure(World world, BlockPos bottom, int height) {
        for (int y = 0; y < height; y++) {
            BlockPos p = bottom.up(y);
            BlockState state = world.getBlockState(p);

            if (!state.isReplaceable()) {
                return false;
            }
        }

        return true;
    }

    private static void placeStructure(World world, BlockPos bottom, int height) {
        world.setBlockState(bottom, ModBlocks.ELEVATOR_CORE_BLOCK.getDefaultState(), Block.NOTIFY_ALL);

        if (world.getBlockEntity(bottom) instanceof ElevatorCoreBlockEntity be) {
            be.initializeStructure(height);
        }

        for (int y = 1; y < height; y++) {
            world.setBlockState(
                    bottom.up(y),
                    ElevatorPartBlock.getStateForFloor(y, height),
                    Block.NOTIFY_ALL
            );
        }
    }
}

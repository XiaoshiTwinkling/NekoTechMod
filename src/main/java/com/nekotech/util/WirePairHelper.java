package com.nekotech.util;

import com.nekotech.block.entity.api.electrical.conductor.ConductorSystem;
import com.nekotech.data.worlddata.ConductorWorldState;
import com.nekotech.item.ModItems;
import com.nekotech.item.custom.WireBundleItem;
import com.nekotech.network.payload.s2c.SyncWirePairsPayload;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class WirePairHelper {
    private WirePairHelper() {
    }

    public static List<ConductorWorldState.WirePairData> removePairAt(World world, BlockPos pos, Direction side) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return List.of();
        }

        ConductorWorldState state = ConductorWorldState.get(serverWorld.getServer());
        List<ConductorWorldState.WirePairData> removedPairs = state.removeWirePairAt(pos, side);
        WireBundleItem.clearFirstClickAt(pos, side);
        afterPairsRemoved(serverWorld, removedPairs);
        return removedPairs;
    }

    public static List<ConductorWorldState.WirePairData> removePairsInvolving(World world, BlockPos pos) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return List.of();
        }

        ConductorWorldState state = ConductorWorldState.get(serverWorld.getServer());
        List<ConductorWorldState.WirePairData> removedPairs = state.removeWirePairsInvolving(pos);
        WireBundleItem.clearFirstClicksAt(pos);
        afterPairsRemoved(serverWorld, removedPairs);
        return removedPairs;
    }

    public static void dropWireBundles(World world, BlockPos pos, List<ConductorWorldState.WirePairData> pairs) {
        if (world.isClient()) {
            return;
        }

        for (ConductorWorldState.WirePairData pair : pairs) {
            ItemStack stack = getWireBundleByType(pair.wireType);
            if (!stack.isEmpty()) {
                Block.dropStack(world, pos, stack);
            }
        }
    }

    public static void giveWireBundles(PlayerEntity player, List<ConductorWorldState.WirePairData> pairs) {
        if (player.getAbilities().creativeMode) {
            return;
        }

        for (ConductorWorldState.WirePairData pair : pairs) {
            ItemStack stack = getWireBundleByType(pair.wireType);
            if (!stack.isEmpty()) {
                player.giveItemStack(stack);
            }
        }
    }

    public static ItemStack getWireBundleByType(String wireType) {
        return switch (wireType) {
            case "copper" -> new ItemStack(ModItems.COPPER_WIRE_BUNDLE, 1);
            case "brass" -> new ItemStack(ModItems.BRASS_WIRE_BUNDLE, 1);
            case "neko_copper" -> new ItemStack(ModItems.NEKO_COPPER_WIRE_BUNDLE, 1);
            default -> ItemStack.EMPTY;
        };
    }

    private static void afterPairsRemoved(ServerWorld world, List<ConductorWorldState.WirePairData> removedPairs) {
        if (removedPairs.isEmpty()) {
            return;
        }

        notifyConductorChanges(world, removedPairs);
        SyncWirePairsPayload.WirePairSyncHelper.syncAllPairsToPlayers(world.getServer());
    }

    private static void notifyConductorChanges(ServerWorld world, List<ConductorWorldState.WirePairData> removedPairs) {
        Set<BlockPos> affectedPositions = new HashSet<>();
        for (ConductorWorldState.WirePairData pair : removedPairs) {
            affectedPositions.add(pair.pos1);
            affectedPositions.add(pair.pos2);
        }

        for (BlockPos affectedPos : affectedPositions) {
            if (world.getBlockEntity(affectedPos) != null) {
                ConductorSystem.onBlockEntityStateChange(world, affectedPos);
            }
        }
    }
}

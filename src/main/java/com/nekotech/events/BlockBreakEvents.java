package com.nekotech.events;

import com.nekotech.data.worlddata.NekoTagWorldState;
import com.nekotech.util.WirePairHelper;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.server.world.ServerWorld;

public class BlockBreakEvents {
    public static void register() {
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!(world instanceof ServerWorld serverWorld)) {
                return;
            }

            NekoTagWorldState.get(serverWorld.getServer()).clearAt(serverWorld, pos);

            var removedPairs = WirePairHelper.removePairsInvolving(serverWorld, pos);
            if (!player.getAbilities().creativeMode) {
                WirePairHelper.dropWireBundles(serverWorld, pos, removedPairs);
            }
        });
    }
}

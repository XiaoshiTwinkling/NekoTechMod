package com.nekotech.catcamera;

import com.nekotech.data.worlddata.CatCameraChannelWorldState;
import com.nekotech.item.ModItems;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;

import java.util.UUID;

public final class CatCameraChannelService {
    private CatCameraChannelService() {}

    public static void reconcile(CatEntity cat) {
        if (!(cat.getWorld() instanceof ServerWorld world) || !(cat instanceof CatCameraChannelAccess access)) {
            return;
        }

        CatCameraChannelWorldState state = CatCameraChannelWorldState.get(world.getServer());
        UUID currentUuid = cat.getUuid();
        CatCameraChannelData local = access.neko_technology$getCatCameraChannel();
        CatCameraChannelData current = state.get(currentUuid);

        if (local != null && !local.indexedCatUuid().equals(currentUuid)) {
            CatCameraChannelData oldWorld = state.get(local.indexedCatUuid());
            CatCameraChannelData winner = newer(local, oldWorld);
            if (winner != null && winner.active()) {
                long revision = state.allocateRevision();
                state.put(local.indexedCatUuid(), CatCameraChannelData.tombstone(local.indexedCatUuid(), revision));
                local = new CatCameraChannelData(currentUuid, winner.name(), winner.ownerUuid(),
                        dimension(world), cat.getChunkPos().x, cat.getChunkPos().z,
                        state.allocateRevision(), true);
                state.put(currentUuid, local);
            } else if (winner != null) {
                local = CatCameraChannelData.tombstone(currentUuid, winner.revision());
            }
        }

        CatCameraChannelData winner = newer(local, current);
        if (winner == null) {
            access.neko_technology$setCatCameraChannel(null);
            access.neko_technology$setCatCameraChannelReconciled(true);
            return;
        }

        if (winner.active()) {
            CatCameraChannelData conflict = state.findActiveByNormalizedName(winner.normalizedName());
            if (conflict != null && !conflict.indexedCatUuid().equals(currentUuid)) {
                boolean winnerWins = winner.revision() > conflict.revision()
                        || winner.revision() == conflict.revision()
                        && currentUuid.toString().compareTo(conflict.indexedCatUuid().toString()) < 0;
                if (winnerWins) {
                    state.put(conflict.indexedCatUuid(), CatCameraChannelData.tombstone(
                            conflict.indexedCatUuid(), state.allocateRevision()));
                } else {
                    winner = CatCameraChannelData.tombstone(currentUuid, state.allocateRevision());
                }
            }
        }

        if (winner.active()) {
            ChunkPos chunk = cat.getChunkPos();
            winner = winner.withLocation(currentUuid, dimension(world), chunk.x, chunk.z);
        } else if (!winner.indexedCatUuid().equals(currentUuid)) {
            winner = CatCameraChannelData.tombstone(currentUuid, winner.revision());
        }

        state.put(currentUuid, winner);
        access.neko_technology$setCatCameraChannel(winner);
        access.neko_technology$setCatCameraChannelReconciled(true);
    }

    public static boolean create(CatEntity cat, PlayerEntity owner, String name) {
        ItemStack cameraStack = findCameraInInventory(owner);
        if (cameraStack.isEmpty()) {
            return false;
        }

        cameraStack.decrement(1);
        cat.equipStack(EquipmentSlot.HEAD, new ItemStack(ModItems.CAT_CAMERA));

        ServerWorld world = (ServerWorld) cat.getWorld();
        CatCameraChannelWorldState state = CatCameraChannelWorldState.get(world.getServer());
        ChunkPos chunk = cat.getChunkPos();
        CatCameraChannelData data = new CatCameraChannelData(cat.getUuid(), name, owner.getUuid(),
                dimension(world), chunk.x, chunk.z, state.allocateRevision(), true);
        state.put(cat.getUuid(), data);
        ((CatCameraChannelAccess) cat).neko_technology$setCatCameraChannel(data);
        return true;
    }

    public static boolean hasCamera(PlayerEntity player) {
        return !findCameraInInventory(player).isEmpty();
    }

    public static void delete(CatEntity cat) {
        if (!(cat.getWorld() instanceof ServerWorld world) || !(cat instanceof CatCameraChannelAccess access)) {
            return;
        }
        dropCameraIfPresent(cat);
        CatCameraChannelWorldState state = CatCameraChannelWorldState.get(world.getServer());
        CatCameraChannelData tombstone = CatCameraChannelData.tombstone(cat.getUuid(), state.allocateRevision());
        state.put(cat.getUuid(), tombstone);
        access.neko_technology$setCatCameraChannel(tombstone);
        access.neko_technology$setCatCameraChannelReconciled(true);
    }

    public static void deleteForBox(CatEntity cat) {
        if (!(cat.getWorld() instanceof ServerWorld world) || !(cat instanceof CatCameraChannelAccess access)) {
            return;
        }
        dropCameraIfPresent(cat);
        CatCameraChannelWorldState state = CatCameraChannelWorldState.get(world.getServer());
        state.put(cat.getUuid(), CatCameraChannelData.tombstone(cat.getUuid(), state.allocateRevision()));
        access.neko_technology$setCatCameraChannel(null);
        access.neko_technology$setCatCameraChannelReconciled(true);
    }

    public static void updateLocation(CatEntity cat) {
        if (!(cat.getWorld() instanceof ServerWorld world) || !(cat instanceof CatCameraChannelAccess access)) {
            return;
        }
        CatCameraChannelData data = access.neko_technology$getCatCameraChannel();
        if (data == null || !data.active()) {
            return;
        }
        ChunkPos chunk = cat.getChunkPos();
        String dimension = dimension(world);
        if (data.chunkX() != chunk.x || data.chunkZ() != chunk.z || !data.dimension().equals(dimension)) {
            CatCameraChannelData updated = data.withLocation(cat.getUuid(), dimension, chunk.x, chunk.z);
            CatCameraChannelWorldState.get(world.getServer()).put(cat.getUuid(), updated);
            access.neko_technology$setCatCameraChannel(updated);
        }
    }

    private static CatCameraChannelData newer(CatCameraChannelData a, CatCameraChannelData b) {
        if (a == null) return b;
        if (b == null) return a;
        if (a.revision() != b.revision()) return a.revision() > b.revision() ? a : b;
        if (a.active() != b.active()) return a.active() ? b : a;
        return a;
    }

    private static String dimension(ServerWorld world) {
        return world.getRegistryKey().getValue().toString();
    }

    private static void dropCameraIfPresent(CatEntity cat) {
        ItemStack headStack = cat.getEquippedStack(EquipmentSlot.HEAD);
        if (headStack.getItem() == ModItems.CAT_CAMERA) {
            cat.dropStack(headStack);
            cat.equipStack(EquipmentSlot.HEAD, ItemStack.EMPTY);
        }
    }

    private static ItemStack findCameraInInventory(PlayerEntity player) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isOf(ModItems.CAT_CAMERA)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}

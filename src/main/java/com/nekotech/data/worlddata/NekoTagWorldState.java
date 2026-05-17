package com.nekotech.data.worlddata;

import com.nekotech.item.custom.NekoTag.NekoPlacedTag;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NekoTagWorldState extends PersistentState {

    private static final String ID = "neko_tag_world_state";

    private static final String ENTRIES = "entries";
    private static final String DIMENSION = "dimension";
    private static final String POS = "pos";
    private static final String TAGS = "tags";

    private final Map<LocationKey, List<NekoPlacedTag>> tagsByLocation = new HashMap<>();

    public static final PersistentState.Type<NekoTagWorldState> TYPE =
            new PersistentState.Type<>(
                    NekoTagWorldState::new,
                    NekoTagWorldState::fromNbt,
                    DataFixTypes.LEVEL
            );

    public static NekoTagWorldState get(MinecraftServer server) {
        ServerWorld overworld = server.getWorld(World.OVERWORLD);

        if (overworld == null) {
            throw new IllegalStateException("Overworld is not available");
        }

        return overworld.getPersistentStateManager().getOrCreate(TYPE, ID);
    }

    public static NekoTagWorldState fromNbt(
            NbtCompound nbt,
            RegistryWrapper.WrapperLookup registryLookup
    ) {
        NekoTagWorldState state = new NekoTagWorldState();

        NbtList entries = nbt.getList(ENTRIES, NbtElement.COMPOUND_TYPE);

        for (int i = 0; i < entries.size(); i++) {
            NbtCompound entryNbt = entries.getCompound(i);

            String dimension = entryNbt.getString(DIMENSION);
            long posLong = entryNbt.getLong(POS);

            if (dimension == null || dimension.isEmpty()) {
                continue;
            }

            LocationKey key = new LocationKey(dimension, posLong);
            NbtList tagsNbt = entryNbt.getList(TAGS, NbtElement.COMPOUND_TYPE);

            List<NekoPlacedTag> tags = new ArrayList<>();

            for (int j = 0; j < tagsNbt.size(); j++) {
                tags.add(NekoPlacedTag.fromNbt(tagsNbt.getCompound(j)));
            }

            if (!tags.isEmpty()) {
                state.tagsByLocation.put(key, tags);
            }
        }

        return state;
    }

    @Override
    public NbtCompound writeNbt(
            NbtCompound nbt,
            RegistryWrapper.WrapperLookup registryLookup
    ) {
        NbtList entries = new NbtList();

        for (Map.Entry<LocationKey, List<NekoPlacedTag>> mapEntry : tagsByLocation.entrySet()) {
            LocationKey key = mapEntry.getKey();
            List<NekoPlacedTag> tags = mapEntry.getValue();

            if (tags.isEmpty()) {
                continue;
            }

            NbtCompound entryNbt = new NbtCompound();
            entryNbt.putString(DIMENSION, key.dimension());
            entryNbt.putLong(POS, key.posLong());

            NbtList tagsNbt = new NbtList();

            for (NekoPlacedTag tag : tags) {
                tagsNbt.add(tag.toNbt());
            }

            entryNbt.put(TAGS, tagsNbt);
            entries.add(entryNbt);
        }

        nbt.put(ENTRIES, entries);
        return nbt;
    }

    public ToggleResult toggle(ServerWorld world, BlockPos pos, NekoPlacedTag tag) {
        LocationKey key = LocationKey.of(world, pos);
        List<NekoPlacedTag> tags = tagsByLocation.computeIfAbsent(key, ignored -> new ArrayList<>());

        boolean removed = tags.remove(tag);

        if (removed) {
            if (tags.isEmpty()) {
                tagsByLocation.remove(key);
            }

            markDirty();
            return ToggleResult.REMOVED;
        }

        tags.add(tag);
        markDirty();
        return ToggleResult.ADDED;
    }

    public boolean clearAt(ServerWorld world, BlockPos pos) {
        LocationKey key = LocationKey.of(world, pos);

        if (tagsByLocation.remove(key) != null) {
            markDirty();
            return true;
        }

        return false;
    }

    public List<NekoPlacedTag> getTagsAt(ServerWorld world, BlockPos pos) {
        LocationKey key = LocationKey.of(world, pos);
        List<NekoPlacedTag> tags = tagsByLocation.get(key);

        if (tags == null) {
            return List.of();
        }

        return Collections.unmodifiableList(tags);
    }

    public List<LocationKey> findLocations(NekoPlacedTag tag) {
        List<LocationKey> result = new ArrayList<>();

        for (Map.Entry<LocationKey, List<NekoPlacedTag>> entry : tagsByLocation.entrySet()) {
            if (entry.getValue().contains(tag)) {
                result.add(entry.getKey());
            }
        }

        return result;
    }

    public record LocationKey(String dimension, long posLong) {
        public static LocationKey of(ServerWorld world, BlockPos pos) {
            return new LocationKey(
                    world.getRegistryKey().getValue().toString(),
                    pos.asLong()
            );
        }

        public BlockPos toBlockPos() {
            return BlockPos.fromLong(posLong);
        }
    }

    public enum ToggleResult {
        ADDED,
        REMOVED
    }

    public List<TaskCandidate> findTasksNear(
            ServerWorld world,
            BlockPos center,
            String color,
            int radius
    ) {
        if (color == null || color.isEmpty()) {
            return List.of();
        }

        String dimension = world.getRegistryKey().getValue().toString();
        double radiusSq = radius * radius;

        List<TaskCandidate> result = new ArrayList<>();

        for (Map.Entry<LocationKey, List<NekoPlacedTag>> entry : tagsByLocation.entrySet()) {
            LocationKey key = entry.getKey();

            if (!key.dimension().equals(dimension)) {
                continue;
            }

            BlockPos taskPos = key.toBlockPos();

            if (taskPos.getSquaredDistance(center) > radiusSq) {
                continue;
            }

            for (NekoPlacedTag tag : entry.getValue()) {
                if (!color.equals(tag.color())) {
                    continue;
                }

                result.add(new TaskCandidate(key, taskPos, tag));
            }
        }

        return result;
    }

    public record TaskCandidate(
            LocationKey location,
            BlockPos pos,
            NekoPlacedTag tag
    ) {
    }
}

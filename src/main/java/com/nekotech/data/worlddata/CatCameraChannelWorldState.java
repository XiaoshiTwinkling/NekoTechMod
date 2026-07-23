package com.nekotech.data.worlddata;

import com.nekotech.catcamera.CatCameraChannelData;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CatCameraChannelWorldState extends PersistentState {
    private static final String ID = "cat_camera_channels";
    private final Map<UUID, CatCameraChannelData> entries = new HashMap<>();
    private long nextRevision = 1L;

    public static final Type<CatCameraChannelWorldState> TYPE = new Type<>(
            CatCameraChannelWorldState::new,
            CatCameraChannelWorldState::fromNbt,
            DataFixTypes.LEVEL
    );

    public static CatCameraChannelWorldState get(MinecraftServer server) {
        ServerWorld overworld = server.getWorld(World.OVERWORLD);
        if (overworld == null) {
            throw new IllegalStateException("Overworld is not available");
        }
        return overworld.getPersistentStateManager().getOrCreate(TYPE, ID);
    }

    public synchronized long allocateRevision() {
        long revision = nextRevision++;
        markDirty();
        return revision;
    }

    public synchronized CatCameraChannelData get(UUID catUuid) {
        return entries.get(catUuid);
    }

    public synchronized void put(UUID catUuid, CatCameraChannelData data) {
        entries.put(catUuid, data);
        nextRevision = Math.max(nextRevision, data.revision() + 1L);
        markDirty();
    }

    public synchronized CatCameraChannelData findActiveByNormalizedName(String normalizedName) {
        for (CatCameraChannelData entry : entries.values()) {
            if (entry.active() && entry.normalizedName().equals(normalizedName)) {
                return entry;
            }
        }
        return null;
    }

    public synchronized List<CatCameraChannelData> getActiveChannels() {
        List<CatCameraChannelData> result = new ArrayList<>();
        for (CatCameraChannelData entry : entries.values()) {
            if (entry.active()) {
                result.add(entry);
            }
        }
        result.sort(Comparator.comparing(CatCameraChannelData::normalizedName)
                .thenComparing(data -> data.indexedCatUuid().toString()));
        return List.copyOf(result);
    }

    public static CatCameraChannelWorldState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        CatCameraChannelWorldState state = new CatCameraChannelWorldState();
        state.nextRevision = Math.max(1L, nbt.getLong("NextRevision"));
        NbtList list = nbt.getList("Entries", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            CatCameraChannelData data = CatCameraChannelData.fromNbt(list.getCompound(i));
            if (data != null) {
                state.entries.put(data.indexedCatUuid(), data);
                state.nextRevision = Math.max(state.nextRevision, data.revision() + 1L);
            }
        }
        return state;
    }

    @Override
    public synchronized NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        nbt.putLong("NextRevision", nextRevision);
        NbtList list = new NbtList();
        entries.values().stream()
                .sorted(Comparator.comparing(data -> data.indexedCatUuid().toString()))
                .map(CatCameraChannelData::toNbt)
                .forEach(list::add);
        nbt.put("Entries", list);
        return nbt;
    }
}

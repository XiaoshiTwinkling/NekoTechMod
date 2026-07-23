package com.nekotech.data.worlddata;

import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameMode;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CatCameraViewSessionWorldState extends PersistentState {
    private static final String ID = "cat_camera_view_sessions";
    private final Map<UUID, Session> sessions = new HashMap<>();

    public static final Type<CatCameraViewSessionWorldState> TYPE = new Type<>(
            CatCameraViewSessionWorldState::new,
            CatCameraViewSessionWorldState::fromNbt,
            DataFixTypes.LEVEL
    );

    public static CatCameraViewSessionWorldState get(MinecraftServer server) {
        ServerWorld overworld = server.getWorld(World.OVERWORLD);
        if (overworld == null) throw new IllegalStateException("Overworld is not available");
        return overworld.getPersistentStateManager().getOrCreate(TYPE, ID);
    }

    public synchronized Session get(UUID playerUuid) { return sessions.get(playerUuid); }
    public synchronized List<Map.Entry<UUID, Session>> entries() { return new ArrayList<>(sessions.entrySet()); }
    public synchronized void put(UUID playerUuid, Session session) { sessions.put(playerUuid, session); markDirty(); }
    public synchronized void remove(UUID playerUuid) { if (sessions.remove(playerUuid) != null) markDirty(); }

    public static CatCameraViewSessionWorldState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        CatCameraViewSessionWorldState state = new CatCameraViewSessionWorldState();
        NbtList list = nbt.getList("Sessions", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound entry = list.getCompound(i);
            if (entry.containsUuid("Player") && entry.containsUuid("Target")) {
                state.sessions.put(entry.getUuid("Player"), Session.fromNbt(entry));
            }
        }
        return state;
    }

    @Override
    public synchronized NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        NbtList list = new NbtList();
        for (Map.Entry<UUID, Session> entry : sessions.entrySet()) {
            NbtCompound sessionNbt = entry.getValue().toNbt();
            sessionNbt.putUuid("Player", entry.getKey());
            list.add(sessionNbt);
        }
        nbt.put("Sessions", list);
        return nbt;
    }

    public record Session(
            UUID targetCatUuid,
            String originDimension,
            double x,
            double y,
            double z,
            float yaw,
            float pitch,
            GameMode gameMode,
            UUID proxyUuid
    ) {
        NbtCompound toNbt() {
            NbtCompound nbt = new NbtCompound();
            nbt.putUuid("Target", targetCatUuid);
            nbt.putString("OriginDimension", originDimension);
            nbt.putDouble("X", x);
            nbt.putDouble("Y", y);
            nbt.putDouble("Z", z);
            nbt.putFloat("Yaw", yaw);
            nbt.putFloat("Pitch", pitch);
            nbt.putString("GameMode", gameMode.getName());
            nbt.putUuid("Proxy", proxyUuid);
            return nbt;
        }

        static Session fromNbt(NbtCompound nbt) {
            return new Session(
                    nbt.getUuid("Target"), nbt.getString("OriginDimension"),
                    nbt.getDouble("X"), nbt.getDouble("Y"), nbt.getDouble("Z"),
                    nbt.getFloat("Yaw"), nbt.getFloat("Pitch"),
                    GameMode.byName(nbt.getString("GameMode"), GameMode.SURVIVAL),
                    nbt.containsUuid("Proxy") ? nbt.getUuid("Proxy") : new UUID(0L, 0L)
            );
        }
    }
}

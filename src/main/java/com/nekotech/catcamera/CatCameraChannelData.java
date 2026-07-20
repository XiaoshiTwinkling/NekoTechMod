package com.nekotech.catcamera;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;

import java.util.Locale;
import java.util.UUID;

public record CatCameraChannelData(
        UUID indexedCatUuid,
        String name,
        UUID ownerUuid,
        String dimension,
        int chunkX,
        int chunkZ,
        long revision,
        boolean active
) {
    public static final String NBT_KEY = "NekoCatCameraChannel";

    public CatCameraChannelData withLocation(UUID catUuid, String dimension, int chunkX, int chunkZ) {
        return new CatCameraChannelData(catUuid, name, ownerUuid, dimension, chunkX, chunkZ, revision, active);
    }

    public CatCameraChannelData withRevisionAndUuid(long newRevision, UUID catUuid) {
        return new CatCameraChannelData(catUuid, name, ownerUuid, dimension, chunkX, chunkZ, newRevision, active);
    }

    public static CatCameraChannelData tombstone(UUID catUuid, long revision) {
        return new CatCameraChannelData(catUuid, "", new UUID(0L, 0L), "", 0, 0, revision, false);
    }

    public String normalizedName() {
        return normalizeName(name);
    }

    public static String normalizeName(String value) {
        return value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putUuid("IndexedCatUuid", indexedCatUuid);
        nbt.putString("Name", name == null ? "" : name);
        nbt.putUuid("OwnerUuid", ownerUuid == null ? new UUID(0L, 0L) : ownerUuid);
        nbt.putString("Dimension", dimension == null ? "" : dimension);
        nbt.putInt("ChunkX", chunkX);
        nbt.putInt("ChunkZ", chunkZ);
        nbt.putLong("Revision", revision);
        nbt.putBoolean("Active", active);
        return nbt;
    }

    public static CatCameraChannelData fromNbt(NbtCompound nbt) {
        if (!nbt.containsUuid("IndexedCatUuid") || !nbt.contains("Revision", NbtElement.LONG_TYPE)) {
            return null;
        }

        UUID owner = nbt.containsUuid("OwnerUuid") ? nbt.getUuid("OwnerUuid") : new UUID(0L, 0L);
        return new CatCameraChannelData(
                nbt.getUuid("IndexedCatUuid"),
                nbt.getString("Name"),
                owner,
                nbt.getString("Dimension"),
                nbt.getInt("ChunkX"),
                nbt.getInt("ChunkZ"),
                nbt.getLong("Revision"),
                nbt.getBoolean("Active")
        );
    }
}

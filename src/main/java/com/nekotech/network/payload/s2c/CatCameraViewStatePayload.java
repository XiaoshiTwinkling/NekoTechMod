package com.nekotech.network.payload.s2c;

import com.nekotech.NekoTechnology;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record CatCameraViewStatePayload(boolean active, UUID catUuid) implements CustomPayload {
    private static final UUID EMPTY = new UUID(0L, 0L);
    public static final Id<CatCameraViewStatePayload> ID = new Id<>(Identifier.of(NekoTechnology.MOD_ID, "cat_camera_view_state"));
    public static final PacketCodec<RegistryByteBuf, CatCameraViewStatePayload> CODEC = PacketCodec.of(
            (payload, buf) -> { buf.writeBoolean(payload.active); buf.writeUuid(payload.catUuid == null ? EMPTY : payload.catUuid); },
            buf -> new CatCameraViewStatePayload(buf.readBoolean(), buf.readUuid())
    );
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}

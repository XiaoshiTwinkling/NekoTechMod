package com.nekotech.network.payload.c2s;

import com.nekotech.NekoTechnology;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record CreateCatCameraChannelPayload(UUID catUuid, String name) implements CustomPayload {
    public static final Id<CreateCatCameraChannelPayload> ID = new Id<>(Identifier.of(NekoTechnology.MOD_ID, "create_cat_camera_channel"));
    public static final PacketCodec<RegistryByteBuf, CreateCatCameraChannelPayload> CODEC = PacketCodec.of(
            (payload, buf) -> { buf.writeUuid(payload.catUuid); buf.writeString(payload.name, 64); },
            buf -> new CreateCatCameraChannelPayload(buf.readUuid(), buf.readString(64))
    );
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}

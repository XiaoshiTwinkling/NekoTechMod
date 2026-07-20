package com.nekotech.network.payload.s2c;

import com.nekotech.NekoTechnology;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record OpenCatCameraNamePayload(UUID catUuid) implements CustomPayload {
    public static final Id<OpenCatCameraNamePayload> ID = new Id<>(Identifier.of(NekoTechnology.MOD_ID, "open_cat_camera_name"));
    public static final PacketCodec<RegistryByteBuf, OpenCatCameraNamePayload> CODEC =
            PacketCodec.of((payload, buf) -> buf.writeUuid(payload.catUuid), buf -> new OpenCatCameraNamePayload(buf.readUuid()));

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}

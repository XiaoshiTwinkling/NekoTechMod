package com.nekotech.network.payload.c2s;

import com.nekotech.NekoTechnology;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record EnterCatCameraChannelPayload(UUID catUuid) implements CustomPayload {
    public static final Id<EnterCatCameraChannelPayload> ID = new Id<>(Identifier.of(NekoTechnology.MOD_ID, "enter_cat_camera_channel"));
    public static final PacketCodec<RegistryByteBuf, EnterCatCameraChannelPayload> CODEC =
            PacketCodec.of((payload, buf) -> buf.writeUuid(payload.catUuid), buf -> new EnterCatCameraChannelPayload(buf.readUuid()));
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}

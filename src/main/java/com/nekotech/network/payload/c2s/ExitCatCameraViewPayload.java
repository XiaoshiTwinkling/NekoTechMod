package com.nekotech.network.payload.c2s;

import com.nekotech.NekoTechnology;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ExitCatCameraViewPayload() implements CustomPayload {
    public static final Id<ExitCatCameraViewPayload> ID = new Id<>(Identifier.of(NekoTechnology.MOD_ID, "exit_cat_camera_view"));
    public static final PacketCodec<RegistryByteBuf, ExitCatCameraViewPayload> CODEC =
            PacketCodec.of((payload, buf) -> {}, buf -> new ExitCatCameraViewPayload());
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}

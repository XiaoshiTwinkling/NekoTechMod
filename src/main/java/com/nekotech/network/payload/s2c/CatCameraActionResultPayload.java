package com.nekotech.network.payload.s2c;

import com.nekotech.NekoTechnology;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record CatCameraActionResultPayload(boolean success, boolean closeScreen, String messageKey) implements CustomPayload {
    public static final Id<CatCameraActionResultPayload> ID = new Id<>(Identifier.of(NekoTechnology.MOD_ID, "cat_camera_action_result"));
    public static final PacketCodec<RegistryByteBuf, CatCameraActionResultPayload> CODEC = PacketCodec.of(
            (payload, buf) -> { buf.writeBoolean(payload.success); buf.writeBoolean(payload.closeScreen); buf.writeString(payload.messageKey, 128); },
            buf -> new CatCameraActionResultPayload(buf.readBoolean(), buf.readBoolean(), buf.readString(128))
    );
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}

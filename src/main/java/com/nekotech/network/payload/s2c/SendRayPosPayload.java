package com.nekotech.network.payload.s2c;

import com.nekotech.NekoTechnology;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record SendRayPosPayload(UUID uuid, double x, double y, double z) implements CustomPayload {
    public static final CustomPayload.Id<SendRayPosPayload> ID =
            new CustomPayload.Id<>(Identifier.of(NekoTechnology.MOD_ID, "send_ray_pos"));

    public static final PacketCodec<RegistryByteBuf, SendRayPosPayload> CODEC =
            PacketCodec.ofStatic(
                    (buf, value) -> {
                        buf.writeUuid(value.uuid());
                        buf.writeDouble(value.x());
                        buf.writeDouble(value.y());
                        buf.writeDouble(value.z());
                    },
                    buf -> new SendRayPosPayload(
                            buf.readUuid(),
                            buf.readDouble(),
                            buf.readDouble(),
                            buf.readDouble()
                    )
            );

    @Override
    public @NotNull Id<? extends CustomPayload> getId() {
        return ID;
    }
}

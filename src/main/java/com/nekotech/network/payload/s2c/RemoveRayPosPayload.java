package com.nekotech.network.payload.s2c;

import com.nekotech.NekoTechnology;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record RemoveRayPosPayload(UUID uuid) implements CustomPayload {
    public static final CustomPayload.Id<RemoveRayPosPayload> ID =
            new CustomPayload.Id<>(Identifier.of(NekoTechnology.MOD_ID, "remove_ray_pos"));

    public static final PacketCodec<RegistryByteBuf, RemoveRayPosPayload> CODEC =
            PacketCodec.ofStatic(
                    (buf, value) -> buf.writeUuid(value.uuid()),
                    buf -> new RemoveRayPosPayload(buf.readUuid())
            );

    @Override
    public @NotNull Id<? extends CustomPayload> getId() {
        return ID;
    }
}

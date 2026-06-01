package com.nekotech.network.payload.c2s;

import com.nekotech.NekoTechnology;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;

public record RequestHudDataPayload(BlockPos pos) implements CustomPayload {
    public static final CustomPayload.Id<RequestHudDataPayload> ID =
            new CustomPayload.Id<>(Identifier.of(NekoTechnology.MOD_ID, "request_hud_data"));

    public static final PacketCodec<RegistryByteBuf, RequestHudDataPayload> CODEC =
            PacketCodec.ofStatic(
                    (buf, value) -> buf.writeBlockPos(value.pos()),
                    buf -> new RequestHudDataPayload(buf.readBlockPos())
            );

    @Override
    public @NotNull Id<? extends CustomPayload> getId() {
        return ID;
    }
}

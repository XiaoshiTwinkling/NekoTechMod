package com.nekotech.network.payload.c2s;

import com.nekotech.NekoTechnology;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

public record NekoTagUpdatePayload(
        String color,
        short priority,
        String task,
        String displayItemId,
        String hand
) implements CustomPayload {
    public static final CustomPayload.Id<NekoTagUpdatePayload> ID =
            new CustomPayload.Id<>(Identifier.of(NekoTechnology.MOD_ID, "neko_tag_update"));

    public static final PacketCodec<RegistryByteBuf, NekoTagUpdatePayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.STRING,
                    NekoTagUpdatePayload::color,

                    PacketCodecs.SHORT,
                    NekoTagUpdatePayload::priority,

                    PacketCodecs.STRING,
                    NekoTagUpdatePayload::task,

                    PacketCodecs.STRING,
                    NekoTagUpdatePayload::displayItemId,

                    PacketCodecs.STRING,
                    NekoTagUpdatePayload::hand,

                    NekoTagUpdatePayload::new
            );

    @Override
    public @NotNull Id<? extends CustomPayload> getId() {
        return ID;
    }
}

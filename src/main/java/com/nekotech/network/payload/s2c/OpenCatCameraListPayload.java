package com.nekotech.network.payload.s2c;

import com.nekotech.NekoTechnology;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record OpenCatCameraListPayload(List<Entry> channels) implements CustomPayload {
    public static final Id<OpenCatCameraListPayload> ID = new Id<>(Identifier.of(NekoTechnology.MOD_ID, "open_cat_camera_list"));
    public static final PacketCodec<RegistryByteBuf, OpenCatCameraListPayload> CODEC = PacketCodec.of(
            OpenCatCameraListPayload::write,
            OpenCatCameraListPayload::read
    );

    private static void write(OpenCatCameraListPayload payload, RegistryByteBuf buf) {
        buf.writeVarInt(payload.channels.size());
        for (Entry entry : payload.channels) {
            buf.writeUuid(entry.catUuid);
            buf.writeString(entry.name, 64);
            buf.writeString(entry.dimension, 128);
        }
    }

    private static OpenCatCameraListPayload read(RegistryByteBuf buf) {
        int count = buf.readVarInt();
        if (count < 0 || count > 4096) throw new IllegalArgumentException("Invalid cat camera channel count: " + count);
        List<Entry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            entries.add(new Entry(buf.readUuid(), buf.readString(64), buf.readString(128)));
        }
        return new OpenCatCameraListPayload(List.copyOf(entries));
    }

    @Override public Id<? extends CustomPayload> getId() { return ID; }
    public record Entry(UUID catUuid, String name, String dimension) {}
}

package com.nekotech.network.payload.s2c;

import com.nekotech.NekoTechnology;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public record OpenTagListPayload(
        BlockPos pos,
        List<TagEntry> tags
) implements CustomPayload {

    public static final CustomPayload.Id<OpenTagListPayload> ID =
            new CustomPayload.Id<>(Identifier.of(NekoTechnology.MOD_ID, "open_tag_list"));

    public static final PacketCodec<RegistryByteBuf, OpenTagListPayload> CODEC =
            PacketCodec.of(OpenTagListPayload::write, OpenTagListPayload::new);

    private OpenTagListPayload(RegistryByteBuf buf) {
        this(buf.readBlockPos(), readTags(buf));
    }

    private void write(RegistryByteBuf buf) {
        buf.writeBlockPos(pos);

        buf.writeVarInt(tags.size());

        for (TagEntry tag : tags) {
            tag.write(buf);
        }
    }

    private static List<TagEntry> readTags(RegistryByteBuf buf) {
        int size = buf.readVarInt();

        if (size < 0 || size > 512) {
            throw new IllegalArgumentException("Invalid neko tag count: " + size);
        }

        List<TagEntry> result = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            result.add(TagEntry.read(buf));
        }

        return result;
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public record TagEntry(
            String color,
            short priority,
            String task,
            String displayStackId
    ) {
        private static TagEntry read(RegistryByteBuf buf) {
            return new TagEntry(
                    buf.readString(64),
                    buf.readShort(),
                    buf.readString(64),
                    buf.readString(128)
            );
        }

        private void write(RegistryByteBuf buf) {
            buf.writeString(color == null ? "" : color);
            buf.writeShort(priority);
            buf.writeString(task == null ? "" : task);
            buf.writeString(displayStackId == null ? "" : displayStackId);
        }
    }
}

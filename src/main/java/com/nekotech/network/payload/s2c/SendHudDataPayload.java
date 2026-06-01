package com.nekotech.network.payload.s2c;

import com.nekotech.NekoTechnology;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;

public record SendHudDataPayload(BlockPos pos, NbtCompound data) implements CustomPayload {
    public static final CustomPayload.Id<SendHudDataPayload> ID =
            new CustomPayload.Id<>(Identifier.of(NekoTechnology.MOD_ID, "send_hud_data"));

    public static final PacketCodec<RegistryByteBuf, SendHudDataPayload> CODEC =
            PacketCodec.ofStatic(
                    (buf, value) -> {
                        buf.writeBlockPos(value.pos());
                        buf.writeNbt(value.data());
                    },
                    buf -> new SendHudDataPayload(buf.readBlockPos(), buf.readNbt())
            );

    @Override
    public @NotNull Id<? extends CustomPayload> getId() {
        return ID;
    }
}

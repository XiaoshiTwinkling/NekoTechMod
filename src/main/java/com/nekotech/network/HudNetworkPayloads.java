package com.nekotech.network;

import com.nekotech.NekoTechnology;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;

public class HudNetworkPayloads {
    // 网络包ID
    public static final CustomPayload.Id<RequestHudDataPayload> REQUEST_HUD_DATA =
            new CustomPayload.Id<>(Identifier.of(NekoTechnology.MOD_ID, "request_hud_data"));
    public static final CustomPayload.Id<SendHudDataPayload> SEND_HUD_DATA =
            new CustomPayload.Id<>(Identifier.of(NekoTechnology.MOD_ID, "send_hud_data"));

    // 1. 客户端→服务端：请求HUD数据的包
    public record RequestHudDataPayload(BlockPos pos) implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, RequestHudDataPayload> CODEC =
                PacketCodec.ofStatic(
                        (buf, value) -> buf.writeBlockPos(value.pos()),
                        buf -> new RequestHudDataPayload(buf.readBlockPos())
                );

        @Override
        public @NotNull Id<? extends CustomPayload> getId() {
            return REQUEST_HUD_DATA;
        }
    }

    // 2. 服务端→客户端：发送HUD数据的包
    public record SendHudDataPayload(BlockPos pos, NbtCompound data) implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, SendHudDataPayload> CODEC =
                PacketCodec.ofStatic(
                        (buf, value) -> {
                            buf.writeBlockPos(value.pos);
                            buf.writeNbt(value.data);
                        },
                        buf -> new SendHudDataPayload(buf.readBlockPos(), buf.readNbt())
                );

        @Override
        public @NotNull Id<? extends CustomPayload> getId() {
            return SEND_HUD_DATA;
        }
    }
}

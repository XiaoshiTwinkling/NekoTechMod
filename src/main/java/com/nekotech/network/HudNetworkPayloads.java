package com.nekotech.network;

import com.nekotech.NekoTechnology;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class HudNetworkPayloads {
    // 网络包ID
    public static final CustomPayload.Id<RequestHudDataPayload> REQUEST_HUD_DATA =
            new CustomPayload.Id<>(Identifier.of(NekoTechnology.MOD_ID, "request_hud_data"));
    public static final CustomPayload.Id<SendHudDataPayload> SEND_HUD_DATA =
            new CustomPayload.Id<>(Identifier.of(NekoTechnology.MOD_ID, "send_hud_data"));
    public static final CustomPayload.Id<SendRayPosPayload> SEND_RAY_POS =
            new CustomPayload.Id<>(Identifier.of(NekoTechnology.MOD_ID, "send_ray_pos"));

    public static final CustomPayload.Id<RemoveRayPosPayload> REMOVE_RAY_POS =
            new CustomPayload.Id<>(Identifier.of(NekoTechnology.MOD_ID, "remove_ray_pos"));

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

    public record SendRayPosPayload(UUID uuid, double x, double y, double z) implements CustomPayload {

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
            return SEND_RAY_POS;
        }
    }

    public record RemoveRayPosPayload(UUID uuid) implements CustomPayload {

        public static final PacketCodec<RegistryByteBuf, RemoveRayPosPayload> CODEC =
                PacketCodec.ofStatic(
                        (buf, value) -> buf.writeUuid(value.uuid()),
                        buf -> new RemoveRayPosPayload(buf.readUuid())
                );

        @Override
        public @NotNull Id<? extends CustomPayload> getId() {
            return REMOVE_RAY_POS;
        }
    }
}

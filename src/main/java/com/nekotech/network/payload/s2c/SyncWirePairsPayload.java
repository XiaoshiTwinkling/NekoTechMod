package com.nekotech.network.payload.s2c;

import com.nekotech.NekoTechnology;
import com.nekotech.data.worlddata.ConductorWorldState;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record SyncWirePairsPayload(List<WirePairData> pairs) implements CustomPayload {
    public static final CustomPayload.Id<SyncWirePairsPayload> ID =
            new CustomPayload.Id<>(Identifier.of(NekoTechnology.MOD_ID, "sync_wire_pairs"));

    public static final PacketCodec<RegistryByteBuf, SyncWirePairsPayload> CODEC =
            PacketCodec.ofStatic(
                    (buf, value) -> {
                        buf.writeVarInt(value.pairs().size());
                        for (WirePairData pair : value.pairs()) {
                            buf.writeBlockPos(pair.pos1());
                            buf.writeEnumConstant(pair.side1());
                            buf.writeBlockPos(pair.pos2());
                            buf.writeEnumConstant(pair.side2());
                            buf.writeString(pair.wireType());
                        }
                    },
                    buf -> {
                        int size = buf.readVarInt();
                        List<WirePairData> list = new ArrayList<>(size);
                        for (int i = 0; i < size; i++) {
                            BlockPos pos1 = buf.readBlockPos();
                            Direction side1 = buf.readEnumConstant(Direction.class);
                            BlockPos pos2 = buf.readBlockPos();
                            Direction side2 = buf.readEnumConstant(Direction.class);
                            String wireType = buf.readString();
                            list.add(new WirePairData(pos1, side1, pos2, side2, wireType));
                        }
                        return new SyncWirePairsPayload(list);
                    }
            );

    public record WirePairData(BlockPos pos1, Direction side1, BlockPos pos2, Direction side2, String wireType) {}

    @Override
    public @NotNull Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static class WirePairSyncHelper {
        public static void syncAllPairsToPlayers(MinecraftServer server) {
            if (server == null) return;
            SyncWirePairsPayload payload = new SyncWirePairsPayload(buildPairList(server));

            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                ServerPlayNetworking.send(player, payload);
            }
        }

        public static void syncPairsToPlayer(MinecraftServer server, ServerPlayerEntity player) {
            if (server == null || player == null) return;
            ServerPlayNetworking.send(player, new SyncWirePairsPayload(buildPairList(server)));
        }

        private static List<SyncWirePairsPayload.WirePairData> buildPairList(MinecraftServer server) {
            ConductorWorldState state = ConductorWorldState.get(server);
            if (state == null) return List.of();

            List<SyncWirePairsPayload.WirePairData> pairList = new ArrayList<>();
            Set<String> seenPairs = new HashSet<>();
            for (ConductorWorldState.WirePairData data : state.getWirePairs().values()) {
                if (seenPairs.add(ConductorWorldState.generatePhysicalWirePairKey(data))) {
                    pairList.add(new SyncWirePairsPayload.WirePairData(
                            data.pos1, data.side1, data.pos2, data.side2, data.wireType
                    ));
                }
            }

            return pairList;
        }
    }
}

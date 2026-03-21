package com.nekotech.data;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.util.math.BlockPos;

public record HeaterData(BlockPos pos) implements  BlockPosPayload{
    public static final PacketCodec<ByteBuf, HeaterData>
            CODEC = PacketCodec.tuple(BlockPos.PACKET_CODEC, HeaterData::pos, HeaterData::new);


}

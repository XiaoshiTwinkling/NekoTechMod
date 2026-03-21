package com.nekotech.data;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.nekotech.item.custom.AlloyFurnace;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.util.math.BlockPos;

public record AlloyFurnaceData(BlockPos pos) implements  BlockPosPayload{
    public static final PacketCodec<ByteBuf, AlloyFurnaceData>
            CODEC = PacketCodec.tuple(BlockPos.PACKET_CODEC, AlloyFurnaceData::pos, AlloyFurnaceData::new);


}

package com.nekotech.block.entity.api.electrical.conductor;

import net.minecraft.util.math.BlockPos;

import java.util.Objects;

/**
 * 能量端口，表示能量流入或流出的点
 */
public class Port {
    public enum Type { INPUT, OUTPUT }

    public final Type type;
    public final float maxRate;      // 最大传输速率 (NF/tick)
    public final float efficiency;   // 效率系数
    public final BlockPos machinePos; // 连接的机器位置
    public final boolean isSelf;     // 是否自身安装
    public final BlockPos portPos;   // 端口所在位置
    public final String sourceItemId; // 源物品ID，用于调试

    public Port(Type type, float maxRate, BlockPos machinePos, boolean isSelf,
                BlockPos portPos, String sourceItemId) {
        this.type = type;
        this.maxRate = maxRate;
        this.efficiency = 1.0f;
        this.machinePos = machinePos;
        this.isSelf = isSelf;
        this.portPos = portPos;
        this.sourceItemId = sourceItemId;
    }

    public Port(Type type, float maxRate, float efficiency, BlockPos machinePos,
                boolean isSelf, BlockPos portPos, String sourceItemId) {
        this.type = type;
        this.maxRate = maxRate;
        this.efficiency = efficiency;
        this.machinePos = machinePos;
        this.isSelf = isSelf;
        this.portPos = portPos;
        this.sourceItemId = sourceItemId;
    }

    public float getEffectiveRate() {
        return maxRate * efficiency;
    }

    @Override
    public String toString() {
        return String.format("Port[%s@%s->%s rate=%.1f]",
                type, portPos, machinePos, maxRate);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Port port = (Port) o;
        return type == port.type &&
                machinePos.equals(port.machinePos) &&
                portPos.equals(port.portPos) &&
                isSelf == port.isSelf;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, machinePos, portPos, isSelf);
    }
}

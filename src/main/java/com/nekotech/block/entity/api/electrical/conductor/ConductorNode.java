package com.nekotech.block.entity.api.electrical.conductor;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.HashSet;
import java.util.Set;

/**
 * 表示导体中的一个节点（一个导体方块）
 */
public class ConductorNode {
    public final BlockPos pos;
    public final Set<Direction> connections = new HashSet<>();
    public Port inputPort = null;
    public Port outputPort = null;
    public boolean isActive = true;
    public float storedEnergy = 0;

    public ConductorNode(BlockPos pos) {
        this.pos = pos;
    }

    public void addConnection(Direction dir) {
        connections.add(dir);
    }

    public void removeConnection(Direction dir) {
        connections.remove(dir);
    }

    public boolean hasPort() {
        return inputPort != null || outputPort != null;
    }

    public boolean isConnectedTo(ConductorNode other) {
        int dx = Math.abs(pos.getX() - other.pos.getX());
        int dy = Math.abs(pos.getY() - other.pos.getY());
        int dz = Math.abs(pos.getZ() - other.pos.getZ());

        return (dx + dy + dz) == 1; // 相邻
    }

    @Override
    public String toString() {
        return String.format("ConductorNode{%s, connections=%d, in=%s, out=%s}",
                pos, connections.size(), inputPort != null, outputPort != null);
    }
}

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
    public final Set<BlockPos> virtualConnections = new HashSet<>(); // 新增：虚拟连接
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

    public void addVirtualConnection(BlockPos targetPos) { // 新增：虚拟连接
        virtualConnections.add(targetPos);
    }

    public void removeConnection(Direction dir) {
        connections.remove(dir);
    }

    public void removeVirtualConnection(BlockPos targetPos) { // 新增
        virtualConnections.remove(targetPos);
    }

    public boolean hasPort() {
        return inputPort != null || outputPort != null;
    }

    public boolean isConnectedTo(ConductorNode other) {
        int dx = Math.abs(pos.getX() - other.pos.getX());
        int dy = Math.abs(pos.getY() - other.pos.getY());
        int dz = Math.abs(pos.getZ() - other.pos.getZ());

        return (dx + dy + dz) == 1; // 物理相邻
    }

    public boolean isVirtuallyConnectedTo(ConductorNode other) { // 新增：虚拟连接检查
        return virtualConnections.contains(other.pos);
    }

    @Override
    public String toString() {
        return String.format("ConductorNode{%s, connections=%d, virtual=%d, in=%s, out=%s}",
                pos, connections.size(), virtualConnections.size(), inputPort != null, outputPort != null);
    }
}
package com.nekotech.block.entity.api.electrical.conductor;

import com.nekotech.NekoTechnology;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.*;

/**
 * 表示一个导体组（由多个导体方块连接而成的整体）
 */
public class ConductorGroup {
    private static int NEXT_ID = 0;

    public final int id;
    public final Set<ConductorNode> nodes = new HashSet<>();
    public final Set<Port> inputPorts = new HashSet<>();
    public final Set<Port> outputPorts = new HashSet<>();

    // 导体组属性
    public float totalInputCapacity = 0;
    public float totalOutputCapacity = 0;
    public float currentLoad = 0;
    public float energyBuffer = 0; // 导体组内部缓冲区

    public ConductorGroup() {
        this.id = NEXT_ID++;
        NekoTechnology.LOGGER.info("[导体组] 创建新导体组 #{}", id);
    }

    public void addNode(ConductorNode node) {
        nodes.add(node);
        updatePortsFromNode(node);
    }

    public void removeNode(ConductorNode node) {
        nodes.remove(node);
        removePortsFromNode(node);
    }

    public void updatePortsFromNode(ConductorNode node) {
        if (node.inputPort != null) {
            inputPorts.add(node.inputPort);
            totalInputCapacity += node.inputPort.getEffectiveRate();
        }
        if (node.outputPort != null) {
            outputPorts.add(node.outputPort);
            totalOutputCapacity += node.outputPort.getEffectiveRate();
        }
    }

    public void removePortsFromNode(ConductorNode node) {
        if (node.inputPort != null) {
            inputPorts.remove(node.inputPort);
            totalInputCapacity -= node.inputPort.getEffectiveRate();
        }
        if (node.outputPort != null) {
            outputPorts.remove(node.outputPort);
            totalOutputCapacity -= node.outputPort.getEffectiveRate();
        }
    }

    public boolean contains(BlockPos pos) {
        for (ConductorNode node : nodes) {
            if (node.pos.equals(pos)) {
                return true;
            }
        }
        return false;
    }

    public ConductorNode getNode(BlockPos pos) {
        for (ConductorNode node : nodes) {
            if (node.pos.equals(pos)) {
                return node;
            }
        }
        return null;
    }

    public Set<ConductorNode> getNeighborNodes(ConductorNode node) {
        Set<ConductorNode> neighbors = new HashSet<>();
        for (Direction dir : node.connections) {
            BlockPos neighborPos = node.pos.offset(dir);
            ConductorNode neighbor = getNode(neighborPos);
            if (neighbor != null) {
                neighbors.add(neighbor);
            }
        }
        return neighbors;
    }

    /**
     * 从起始位置发现并构建导体组
     */
    public void discover(World world, BlockPos startPos, PortScanner portScanner) {
        Queue<BlockPos> queue = new LinkedList<>();
        Set<BlockPos> visited = new HashSet<>();

        queue.offer(startPos);
        visited.add(startPos);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();

            // 创建节点
            ConductorNode node = new ConductorNode(current);
            portScanner.scanPorts(world, node);
            this.addNode(node);

            // 检查六个方向
            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = current.offset(dir);

                if (visited.contains(neighborPos)) continue;

                // 检查是否是导体方块
                if (world.getBlockEntity(neighborPos) instanceof com.nekotech.block.entity.api.electrical.ITransferElectrical) {
                    // 添加连接
                    node.addConnection(dir);
                    queue.offer(neighborPos);
                    visited.add(neighborPos);
                }
            }
        }

        NekoTechnology.LOGGER.info("[导体组#{}] 发现完成，包含 {} 个节点，输入口={}, 输出口={}",
                id, nodes.size(), inputPorts.size(), outputPorts.size());
    }

    /**
     * 合并另一个导体组
     */
    public void merge(ConductorGroup other) {
        for (ConductorNode node : other.nodes) {
            this.nodes.add(node);
            updatePortsFromNode(node);
        }
        other.nodes.clear();
        NekoTechnology.LOGGER.info("[导体组#{}] 合并了导体组#{}", id, other.id);
    }

    /**
     * 在移除指定位置后分割导体组
     * 返回分割后的新导体组列表
     */
    public List<ConductorGroup> splitAt(World world, BlockPos removedPos) {
        // 移除节点
        ConductorNode removedNode = getNode(removedPos);
        if (removedNode != null) {
            removeNode(removedNode);
        }

        // 构建图
        Map<BlockPos, List<BlockPos>> graph = new HashMap<>();
        for (ConductorNode node : nodes) {
            List<BlockPos> neighbors = new ArrayList<>();
            for (Direction dir : node.connections) {
                BlockPos neighborPos = node.pos.offset(dir);
                if (getNode(neighborPos) != null && !neighborPos.equals(removedPos)) {
                    neighbors.add(neighborPos);
                }
            }
            graph.put(node.pos, neighbors);
        }

        // 查找连通分量
        List<Set<BlockPos>> components = findConnectedComponents(graph);

        // 创建新导体组
        List<ConductorGroup> newGroups = new ArrayList<>();
        for (Set<BlockPos> component : components) {
            if (!component.isEmpty()) {
                ConductorGroup newGroup = new ConductorGroup();
                for (BlockPos pos : component) {
                    ConductorNode node = getNode(pos);
                    if (node != null) {
                        newGroup.addNode(node);
                    }
                }
                newGroups.add(newGroup);
            }
        }

        NekoTechnology.LOGGER.info("[导体组#{}] 分割成 {} 个导体组", id, newGroups.size());
        return newGroups;
    }

    private List<Set<BlockPos>> findConnectedComponents(Map<BlockPos, List<BlockPos>> graph) {
        List<Set<BlockPos>> components = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();

        for (BlockPos pos : graph.keySet()) {
            if (!visited.contains(pos)) {
                Set<BlockPos> component = new HashSet<>();
                Queue<BlockPos> queue = new LinkedList<>();
                queue.add(pos);
                visited.add(pos);

                while (!queue.isEmpty()) {
                    BlockPos current = queue.poll();
                    component.add(current);

                    for (BlockPos neighbor : graph.get(current)) {
                        if (!visited.contains(neighbor)) {
                            visited.add(neighbor);
                            queue.add(neighbor);
                        }
                    }
                }

                if (!component.isEmpty()) {
                    components.add(component);
                }
            }
        }

        return components;
    }

    public void tick(World world) {
        if (world.isClient || nodes.isEmpty()) {
            return;
        }

        NekoTechnology.LOGGER.debug("[导体组#{}] 开始tick，输入口={}，输出口={}",
                id, inputPorts.size(), outputPorts.size());

        // 执行能量分配
        EnergyDistributor distributor = new EnergyDistributor();
        EnergyDistributor.AllocationResult result = distributor.distributeEnergy(world, this);

        if (result.totalTransferred > 0 && world.getTime() % 100 == 0) {
            NekoTechnology.LOGGER.info("[导体组#{}] 传输摘要: 传输了 {:.2f} NF",
                    id, result.totalTransferred);
        }
    }

    @Override
    public String toString() {
        return String.format("ConductorGroup#%d{nodes=%d, inputs=%d, outputs=%d}",
                id, nodes.size(), inputPorts.size(), outputPorts.size());
    }
}

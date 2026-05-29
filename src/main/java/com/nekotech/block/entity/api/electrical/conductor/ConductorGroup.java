package com.nekotech.block.entity.api.electrical.conductor;

import com.nekotech.NekoTechnology;
import com.nekotech.block.entity.api.electrical.ITransferElectrical;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.*;

/**
 * 表示一个导体组（由多个导体方块连接而成的整体）
 */
public class ConductorGroup {
    static int NEXT_ID = 0;

    public int id;
    public final Set<ConductorNode> nodes = new HashSet<>();
    public final Set<Port> inputPorts = new HashSet<>();
    public final Set<Port> outputPorts = new HashSet<>();

    // 导体组属性
    public float totalInputCapacity = 0;
    public float totalOutputCapacity = 0;

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
            if (!inputPorts.contains(node.inputPort)) {
                inputPorts.add(node.inputPort);
                totalInputCapacity += node.inputPort.getEffectiveRate();
            }
        }
        if (node.outputPort != null) {
            if (!outputPorts.contains(node.outputPort)) {
                outputPorts.add(node.outputPort);
                totalOutputCapacity += node.outputPort.getEffectiveRate();
            }
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
                if (world.getBlockEntity(neighborPos) instanceof ITransferElectrical) {
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
        float otherInputCapacity = other.totalInputCapacity;
        float otherOutputCapacity = other.totalOutputCapacity;

        for (ConductorNode node : other.nodes) {
            this.nodes.add(node);
            updatePortsFromNode(node);
        }

        this.inputPorts.addAll(other.inputPorts);
        this.outputPorts.addAll(other.outputPorts);
        this.totalInputCapacity += otherInputCapacity;
        this.totalOutputCapacity += otherOutputCapacity;

        other.inputPorts.clear();
        other.outputPorts.clear();
        other.totalInputCapacity = 0;
        other.totalOutputCapacity = 0;
        other.nodes.clear();
    }

    /**
     * 在移除指定位置后分割导体组
     * 返回分割后的新导体组列表
     */
    public List<ConductorGroup> splitAt(World world, BlockPos removedPos) {
        ConductorNode removedNode = getNode(removedPos);
        if (removedNode != null) {
            removeNode(removedNode);
        }

        if (nodes.isEmpty()) {
            NekoTechnology.LOGGER.info("[导体组#{}] 分割后无节点，直接移除", id);
            return Collections.emptyList();
        }

        List<Set<BlockPos>> connectedComponents = findConnectedComponentsAfterRemoval(world, removedPos);

        List<ConductorGroup> newGroups = new ArrayList<>();

        for (Set<BlockPos> component : connectedComponents) {
            if (component.isEmpty()) {
                continue;
            }

            ConductorGroup newGroup = new ConductorGroup();
            PortScanner scanner = new PortScanner();

            // 为这个连通分量中的所有节点创建新组
            for (BlockPos pos : component) {
                ConductorNode originalNode = getNode(pos);
                if (originalNode != null) {
                    // 创建节点副本，避免修改原节点
                    ConductorNode newNode = new ConductorNode(pos);

                    // 复制连接信息
                    for (Direction dir : originalNode.connections) {
                        if (component.contains(pos.offset(dir))) {
                            newNode.addConnection(dir);
                        }
                    }

                    // 复制端口信息
                    newNode.inputPort = originalNode.inputPort;
                    newNode.outputPort = originalNode.outputPort;

                    newGroup.addNode(newNode);
                }
            }

            if (!newGroup.nodes.isEmpty()) {
                newGroups.add(newGroup);
                NekoTechnology.LOGGER.debug("[导体组#{}] 创建新导体组#{}，包含 {} 个节点",
                        id, newGroup.id, newGroup.nodes.size());
            }
        }

        NekoTechnology.LOGGER.info("[导体组#{}] 分割成 {} 个新导体组", id, newGroups.size());
        return newGroups;
    }

    /**
     * 查找移除指定位置后的所有连通分量
     */
    private List<Set<BlockPos>> findConnectedComponentsAfterRemoval(World world, BlockPos removedPos) {
        // 使用BFS查找所有连通分量
        List<Set<BlockPos>> components = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();

        for (ConductorNode node : nodes) {
            if (visited.contains(node.pos)) {
                continue;
            }

            // 对每个未访问的节点进行BFS
            Set<BlockPos> component = new HashSet<>();
            Queue<BlockPos> queue = new LinkedList<>();
            queue.add(node.pos);
            visited.add(node.pos);

            while (!queue.isEmpty()) {
                BlockPos current = queue.poll();
                component.add(current);

                ConductorNode currentNode = getNode(current);
                if (currentNode == null) {
                    continue;
                }

                for (Direction dir : currentNode.connections) {
                    BlockPos neighborPos = current.offset(dir);

                    if (neighborPos.equals(removedPos)) {
                        continue;
                    }

                    if (!visited.contains(neighborPos) && getNode(neighborPos) != null) {
                        visited.add(neighborPos);
                        queue.add(neighborPos);
                    }
                }
            }

            if (!component.isEmpty()) {
                components.add(component);
            }
        }
        return components;
    }

    // 重置ID
    public static void resetNextId(int nextId) {
        NEXT_ID = nextId;
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

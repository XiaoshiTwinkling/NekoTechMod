package com.nekotech.block.entity.api.electrical.conductor;

import com.nekotech.NekoTechnology;
import com.nekotech.block.entity.api.component.ComponentAdaptation;
import com.nekotech.block.entity.api.electrical.ITransferElectrical;
import com.nekotech.data.worlddata.ConductorWorldState;
import com.nekotech.util.WirePairHelper;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * 管理一个世界中的所有导体网络喵~
 * 那我是猫猫王了
 */
public class ConductorManager {
    private static final Map<World, ConductorManager> INSTANCES = new java.util.concurrent.ConcurrentHashMap<>();

    private final Map<BlockPos, ConductorGroup> blockToGroup = new HashMap<>();

    private final Map<Integer, ConductorGroup> conductorGroups = new HashMap<>();
    private final PortScanner portScanner = new PortScanner();

    private final Set<BlockPos> pendingRebuildPositions = new HashSet<>();

    private final Map<BlockPos, Integer> posToGroupId = new HashMap<>();

    // 操作类型枚举
    private enum OperationType {
        PLACE, BREAK, COMPONENT_CHANGE, BLOCK_ENTITY_STATE_CHANGE
    }

    private static class PendingOperation {
        final BlockPos pos;
        final OperationType type;
        final Direction side; // 对于零件变更有用

        PendingOperation(BlockPos pos, OperationType type, Direction side) {
            this.pos = pos;
            this.type = type;
            this.side = side;
        }
    }

    private static int NEXT_ID = 0;
    public int id;

    private final Queue<PendingOperation> operationQueue = new LinkedList<>();
    private final Set<BlockPos> processedPositions = new HashSet<>();

    // 添加对世界状态的引用
    private ConductorWorldState worldState;

    public static void setNextId(int nextId) {
        NEXT_ID = nextId;
        NekoTechnology.LOGGER.info("[导体组] 设置下一个ID为: {}", nextId);
    }

    public static int getNextId() {
        return NEXT_ID;
    }

    public void ConductorGroup() {
        this.id = NEXT_ID++;
        NekoTechnology.LOGGER.info("[导体组] 创建新导体组 #{}, 下一个ID: {}", id, NEXT_ID);
    }

    /**
     * 获取导体管理器实例
     */
    public static ConductorManager get(World world) {
        if (world.isClient) {
            return null;
        }

        // 1. 从缓存获取
        ConductorManager manager = INSTANCES.get(world);
        if (manager != null) {
            return manager;
        }

        // 2. 创建新实例
        manager = new ConductorManager();

        // 3. 获取世界状态
        try {
            MinecraftServer server = ((ServerWorld) world).getServer();
            manager.worldState = ConductorWorldState.get(server);

            // 4. 从世界状态加载数据
            manager.loadFromWorldState();

            NekoTechnology.LOGGER.info("[导体管理器] 从世界状态加载完成: {} 个组",
                    manager.conductorGroups.size());
        } catch (Exception e) {
            NekoTechnology.LOGGER.error("[导体管理器] 加载世界状态失败: {}", e.getMessage());
            // 创建空的世界状态
            manager.worldState = new ConductorWorldState();
        }

        // 5. 放入缓存
        INSTANCES.put(world, manager);

        return manager;
    }

    /**
     * 从 ConductorWorldState 加载数据
     */
    private void loadFromWorldState() {
        if (worldState == null) {
            NekoTechnology.LOGGER.error("[导体管理器] 世界状态为空，无法加载数据");
            return;
        }

        // 1. 清空现有数据
        blockToGroup.clear();
        conductorGroups.clear();
        posToGroupId.clear();

        // 2. 设置下一个组ID
        int nextId = worldState.getNextGroupId();
        if (nextId > 0) {
            setNextId(nextId);
        }

        // 3. 加载所有组
        Map<Integer, ConductorWorldState.ConductorGroupData> worldGroups = worldState.getGroups();

        for (Map.Entry<Integer, ConductorWorldState.ConductorGroupData> entry : worldGroups.entrySet()) {
            int groupId = entry.getKey();
            ConductorWorldState.ConductorGroupData groupData = entry.getValue();

            // 转换数据格式
            ConductorGroup conductorGroup = convertToConductorGroup(groupData);
            if (conductorGroup != null) {
                conductorGroups.put(groupId, conductorGroup);

                // 重建映射
                for (ConductorNode node : conductorGroup.nodes) {
                    blockToGroup.put(node.pos, conductorGroup);
                    posToGroupId.put(node.pos, groupId);
                }
            }
        }

        NekoTechnology.LOGGER.info("[导体管理器] 从世界状态加载完成: 组数={}, 方块映射={}",
                conductorGroups.size(), blockToGroup.size());
    }

    /**
     * 保存当前状态到世界状态
     */
    public void saveToWorldState() {

        if (worldState == null) {
            NekoTechnology.LOGGER.error("[导体管理器] 世界状态为空，无法保存数据");
            return;
        }

        // 1. 设置下一个组ID
        worldState.setNextGroupId(getNextId());

        // 2. 清空旧数据
        worldState.getGroups().clear();

        // 3. 保存所有组
        for (Map.Entry<Integer, ConductorGroup> entry : conductorGroups.entrySet()) {
            int groupId = entry.getKey();
            ConductorGroup conductorGroup = entry.getValue();

            ConductorWorldState.ConductorGroupData groupData = convertToGroupData(conductorGroup);
            worldState.getGroups().put(groupId, groupData);
        }

        // 4. 标记为脏，需要保存
        worldState.markDirty();

        NekoTechnology.LOGGER.debug("[导体管理器] 保存到世界状态: 组数={}, 下一个ID={}",
                conductorGroups.size(), getNextId());
    }

    /**
     * 将世界状态的数据模型转换为运行时模型
     */
    private ConductorGroup convertToConductorGroup(ConductorWorldState.ConductorGroupData groupData) {
        ConductorGroup conductorGroup = new ConductorGroup();

        // 通过反射设置ID
        try {
            java.lang.reflect.Field idField = ConductorGroup.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(conductorGroup, groupData.id);
        } catch (Exception e) {
            NekoTechnology.LOGGER.error("无法设置导体组ID: {}", e.getMessage());
            return null;
        }

        // 转换节点
        for (ConductorWorldState.ConductorNodeData nodeData : groupData.nodes) {
            ConductorNode node = new ConductorNode(nodeData.pos);

            // 连接方向
            node.connections.addAll(nodeData.connections);

            // 输入端口
            if (nodeData.inputPort != null) {
                node.inputPort = convertToPort(nodeData.inputPort);
            }

            // 输出端口
            if (nodeData.outputPort != null) {
                node.outputPort = convertToPort(nodeData.outputPort);
            }

            conductorGroup.nodes.add(node);

            // 更新端口列表
            if (node.inputPort != null) {
                conductorGroup.inputPorts.add(node.inputPort);
            }
            if (node.outputPort != null) {
                conductorGroup.outputPorts.add(node.outputPort);
            }
        }

        return conductorGroup;
    }

    /**
     * 将运行时模型转换为世界状态的数据模型
     */
    private ConductorWorldState.ConductorGroupData convertToGroupData(ConductorGroup conductorGroup) {
        ConductorWorldState.ConductorGroupData groupData =
                new ConductorWorldState.ConductorGroupData(conductorGroup.id);

        for (ConductorNode node : conductorGroup.nodes) {
            ConductorWorldState.ConductorNodeData nodeData =
                    new ConductorWorldState.ConductorNodeData(node.pos);

            // 连接方向
            nodeData.connections.addAll(node.connections);

            // 输入端口
            if (node.inputPort != null) {
                nodeData.inputPort = convertToPortData(node.inputPort);
            }

            // 输出端口
            if (node.outputPort != null) {
                nodeData.outputPort = convertToPortData(node.outputPort);
            }

            groupData.nodes.add(nodeData);
        }

        return groupData;
    }

    /**
     * 将世界状态的端口数据转换为运行时端口
     */
    private Port convertToPort(ConductorWorldState.PortData portData) {
        try {
            return new Port(
                    Port.Type.valueOf(portData.type.name()),
                    portData.maxRate,
                    portData.efficiency,
                    portData.machinePos,
                    portData.isSelf,
                    new BlockPos(portData.machinePos.getX(), portData.machinePos.getY(), portData.machinePos.getZ()),
                    portData.sourceItemId
            );
        } catch (Exception e) {
            NekoTechnology.LOGGER.error("转换端口数据失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 将运行时端口转换为世界状态的端口数据
     */
    private ConductorWorldState.PortData convertToPortData(Port port) {
        return new ConductorWorldState.PortData(
                port.type,
                port.maxRate,
                port.efficiency,
                port.machinePos,
                port.isSelf,
                port.sourceItemId
        );
    }

    /**
     * 当导体方块被放置时调用
     */
    public void onBlockPlaced(World world, BlockPos pos) {
        operationQueue.add(new PendingOperation(pos, OperationType.PLACE, null));
        NekoTechnology.LOGGER.debug("[导体管理器] 缓存方块放置操作: {}", pos);
    }

    /**
     * 当导体方块被破坏时调用
     */
    public void onBlockBroken(World world, BlockPos pos) {
        operationQueue.add(new PendingOperation(pos, OperationType.BREAK, null));
        NekoTechnology.LOGGER.debug("[导体管理器] 缓存方块破坏操作: {}", pos);
    }

    /**
     * 当零件变更时调用
     */
    public void onComponentChanged(World world, BlockPos pos, Direction side) {
        operationQueue.add(new PendingOperation(pos, OperationType.COMPONENT_CHANGE, side));
        NekoTechnology.LOGGER.debug("[导体管理器] 缓存零件变更操作: {} {}", pos, side);
    }

    // 在 ConductorManager 类中添加新的公共方法
    /**
     * 当导体方块实体状态变化时调用喵~
     * 这包括但不限于：断路器开关、机器状态变化、能量水平变化等
     * 使用1tick延迟，避免立即处理导致的问题
     *
     * @param world 世界对象
     * @param pos 方块位置
     */
    public void onBlockEntityStateChange(World world, BlockPos pos) {
        operationQueue.add(new PendingOperation(pos, OperationType.BLOCK_ENTITY_STATE_CHANGE, null));
        NekoTechnology.LOGGER.debug("[导体管理器] 缓存方块实体状态变化操作: {}", pos);
    }

// 添加新的处理方法 handleBlockEntityStateChange
    /**
     * 处理方块实体状态变化喵~
     * 方块实体状态变化可能影响导体网络的连接和能量传输
     * 需要重新扫描端口并更新导体组
     *
     * @param world 世界对象
     * @param pos 发生状态变化的方块位置
     */
    private void handleBlockEntityStateChange(World world, BlockPos pos) {
        NekoTechnology.LOGGER.info("[导体管理器] 处理方块实体状态变化: {}", pos);

        // 获取方块实体
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity == null) {
            NekoTechnology.LOGGER.warn("[导体管理器] 位置 {} 没有方块实体", pos);
            return;
        }

        boolean isWirePole = false;
        if (blockEntity instanceof ComponentAdaptation ca) {
            for (Direction dir : Direction.values()) {
                if (ca.getComponent(dir) instanceof com.nekotech.item.custom.component.WirePoleItem) {
                    isWirePole = true;
                    break;
                }
            }
        }

        if (isWirePole) {
            NekoTechnology.LOGGER.debug("[导体管理器] 接线柱状态变化: {}", pos);
            // 对于接线柱，需要重新扫描整个导体组
            handleWirePoleStateChange(world, pos);
        } else if (blockEntity instanceof ITransferElectrical electrical) {
            // ... 原有的处理逻辑 ...
        } else {
            NekoTechnology.LOGGER.debug("[导体管理器] 方块 {} 不是 ITransferElectrical 实例", pos);
        }

        // 保存到世界状态
        saveToWorldState();
    }

    private void handleWirePoleStateChange(World world, BlockPos pos) {
        // 获取这个接线柱所在的所有导体组
        ConductorGroup group = blockToGroup.get(pos);

        if (group != null) {
            // 重新扫描这个导体组中的所有接线柱配对
            NekoTechnology.LOGGER.info("[导体管理器] 重新扫描导体组#{} 中的接线柱", group.id);

            // 遍历组中的所有节点，重新扫描接线柱配对
            for (ConductorNode node : new HashSet<>(group.nodes)) {
                // 清除旧的虚拟连接
                node.virtualConnections.clear();

                // 重新扫描端口（包括接线柱配对）
                portScanner.scanPorts(world, node);
            }
        } else {
            // 如果接线柱不在任何导体组中，尝试加入或创建导体组
            NekoTechnology.LOGGER.info("[导体管理器] 接线柱 {} 不在任何导体组中，尝试加入", pos);
            handleBlockPlace(world, pos);
        }
    }

    private void processOperation(World world, PendingOperation operation) {
        if (processedPositions.contains(operation.pos)) {
            NekoTechnology.LOGGER.debug("[导体管理器] 跳过已处理的位置: {}", operation.pos);
            return;
        }

        switch (operation.type) {
            case PLACE -> handleBlockPlace(world, operation.pos);
            case BREAK -> handleBlockBreak(world, operation.pos);
            case COMPONENT_CHANGE -> handleComponentChange(world, operation.pos, operation.side);
            case BLOCK_ENTITY_STATE_CHANGE -> handleBlockEntityStateChange(world, operation.pos);
        }

        processedPositions.add(operation.pos);
    }

    private void handleBlockPlace(World world, BlockPos pos) {
        NekoTechnology.LOGGER.info("[导体管理器] 处理方块放置: {}", pos);

        if (!isConductor(world, pos)) {
            return;
        }

        // 查找相邻导体组
        Set<ConductorGroup> adjacentGroups = findAdjacentGroups(world, pos);

        if (adjacentGroups.isEmpty()) {
            // 创建新导体组
            createNewGroup(world, pos);
        } else if (adjacentGroups.size() == 1) {
            // 加入现有导体组
            ConductorGroup group = adjacentGroups.iterator().next();
            expandGroup(world, group, pos);
        } else {
            // 合并多个导体组
            mergeGroups(world, adjacentGroups, pos);
        }

        // 保存到世界状态
        saveToWorldState();
    }

    private void handleBlockBreak(World world, BlockPos pos) {
        NekoTechnology.LOGGER.info("[导体管理器] 处理方块破坏: {}", pos);

        ConductorGroup group = blockToGroup.get(pos);
        if (group == null) {
            NekoTechnology.LOGGER.debug("[导体管理器] 位置 {} 没有对应的导体组", pos);
            return;
        }
        int originalGroupId = group.id;
        int originalNodeCount = group.nodes.size();

        // 调用新的分割逻辑
        List<ConductorGroup> splitGroups = group.splitAt(world, pos);

        // 从映射中移除被破坏的方块
        blockToGroup.remove(pos);

        NekoTechnology.LOGGER.info("[导体管理器] 导体组#{} (原{}节点) 分割成 {} 个新组",
                originalGroupId, originalNodeCount, splitGroups.size());

        if (splitGroups.isEmpty()) {
            // 如果没有产生新组，说明原组已空
            removeGroup(group);
            NekoTechnology.LOGGER.info("[导体管理器] 导体组#{} 变为空，已移除", originalGroupId);
        } else {
            removeGroup(group);

            for (ConductorGroup newGroup : splitGroups) {
                if (!newGroup.nodes.isEmpty()) {
                    registerGroup(newGroup);
                    NekoTechnology.LOGGER.info("[导体管理器] 注册新导体组#{}，节点: {}，输入口: {}，输出口: {}",
                            newGroup.id, newGroup.nodes.size(),
                            newGroup.inputPorts.size(), newGroup.outputPorts.size());
                }
            }
        }

        WirePairHelper.removePairsInvolving(world, pos);

        // 保存到世界状态
        saveToWorldState();
    }

    private void handleComponentChange(World world, BlockPos pos, Direction side) {
        NekoTechnology.LOGGER.info("[导体管理器] 处理零件变更: {} {}", pos, side);

        ConductorGroup group = blockToGroup.get(pos);
        if (group == null) {
            return;
        }

        // 重新扫描端口
        ConductorNode node = group.getNode(pos);
        if (node != null) {
            group.removePortsFromNode(node);
            portScanner.scanPorts(world, node);
            group.updatePortsFromNode(node);
        }

        // 保存到世界状态
        saveToWorldState();
    }

    private boolean isConductor(World world, BlockPos pos) {
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof ITransferElectrical electrical) {
            return electrical.canTransfer();
        }
        return false;
    }

    private Set<ConductorGroup> findAdjacentGroups(World world, BlockPos pos) {
        Set<ConductorGroup> groups = new HashSet<>();

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.offset(dir);
            ConductorGroup group = blockToGroup.get(neighborPos);
            if (group != null && isConductor(world, neighborPos)) {
                groups.add(group);
            }
        }

        return groups;
    }

    private void createNewGroup(World world, BlockPos startPos) {
        ConductorGroup group = new ConductorGroup();
        group.discover(world, startPos, portScanner);
        registerGroup(group);
    }

    private void expandGroup(World world, ConductorGroup group, BlockPos newPos) {
        if (group.contains(newPos)) {
            return;
        }

        // 重新发现整个导体组
        ConductorGroup newGroup = new ConductorGroup();
        newGroup.discover(world, newPos, portScanner);
        group.merge(newGroup);
        updateGroupMapping(group);
    }

    private void mergeGroups(World world, Set<ConductorGroup> groupsToMerge, BlockPos newPos) {
        if (groupsToMerge.isEmpty()) {
            return;
        }

        // 选择第一个导体组作为主导体组
        Iterator<ConductorGroup> iterator = groupsToMerge.iterator();
        ConductorGroup mainGroup = iterator.next();

        // 合并其他导体组
        while (iterator.hasNext()) {
            ConductorGroup group = iterator.next();
            mainGroup.merge(group);
            removeGroup(group);
        }

        // 添加新方块
        expandGroup(world, mainGroup, newPos);
    }

    private void registerGroup(ConductorGroup group) {
        conductorGroups.put(group.id, group);
        for (ConductorNode node : group.nodes) {
            blockToGroup.put(node.pos, group);
        }
        saveToWorldState();  // 保存到世界状态
        NekoTechnology.LOGGER.info("[导体管理器] 注册导体组: {}", group);
    }

    private void removeGroup(ConductorGroup group) {
        conductorGroups.remove(group.id);
        for (ConductorNode node : group.nodes) {
            blockToGroup.remove(node.pos);
        }
        saveToWorldState();  // 保存到世界状态
        NekoTechnology.LOGGER.info("[导体管理器] 移除导体组: {}", group);
    }

    private void updateGroupMapping(ConductorGroup group) {
        for (ConductorNode node : group.nodes) {
            blockToGroup.put(node.pos, group);
        }
    }

    public @Nullable ConductorGroup getGroupAt(BlockPos pos) {
        return blockToGroup.get(pos);
    }

    public Collection<ConductorGroup> getAllGroups() {
        return conductorGroups.values();
    }

    private boolean needsRescan = false;
    private int worldLoadTicks = 0;

    public void tick(World world) {
        // 世界加载后的处理
        if (worldLoadTicks < 20) {  // 等待20个tick，确保世界完全加载
            worldLoadTicks++;
            if (worldLoadTicks == 5) {  // 第5个tick时触发重新扫描
                needsRescan = true;
            }
        }

        // 重新扫描端口
        if (needsRescan) {
            rescanAllPorts(world);
            needsRescan = false;
            NekoTechnology.LOGGER.info("[导体管理器] 世界加载后端口重新扫描完成");
        }

        // 处理操作队列
        int processedCount = 0;
        int maxOperationsPerTick = 5;

        while (!operationQueue.isEmpty() && processedCount < maxOperationsPerTick) {
            PendingOperation operation = operationQueue.poll();
            if (operation != null) {
                processOperation(world, operation);
                processedCount++;
            }
        }

        if (world.getTime() % 20 == 0) {
            processedPositions.clear();
        }

        // 定期保存（每50秒）
        if (world.getTime() % 1000 == 0) {
            saveToWorldState();
        }

        // 处理导体组tick
        for (ConductorGroup group : conductorGroups.values()) {
            if (!group.nodes.isEmpty()) {
                group.tick(world);
            }
        }
    }

    @Override
    public String toString() {
        return String.format("ConductorManager{groups=%d, blocks=%d}",
                conductorGroups.size(), blockToGroup.size());
    }

    /**
     * 重新扫描所有导体组的端口
     * 用于在世界加载后刷新端口信息
     */
    private void rescanAllPorts(World world) {
        NekoTechnology.LOGGER.info("[导体管理器] 重新扫描所有端口...");

        int totalGroups = conductorGroups.size();
        int processedGroups = 0;

        for (ConductorGroup group : conductorGroups.values()) {
            // 临时存储需要移除的节点
            Set<ConductorNode> nodesToRemove = new HashSet<>();

            // 遍历组中所有节点
            for (ConductorNode node : new HashSet<>(group.nodes)) {
                // 检查这个节点是否还是有效的导体
                if (!isConductor(world, node.pos)) {
                    // 如果不再是导体，标记为需要移除
                    nodesToRemove.add(node);
                    continue;
                }

                // 清除旧端口
                group.removePortsFromNode(node);
                node.inputPort = null;
                node.outputPort = null;

                // 重新扫描端口
                portScanner.scanPorts(world, node);

                // 更新端口映射
                group.updatePortsFromNode(node);
            }

            // 移除无效的节点
            for (ConductorNode node : nodesToRemove) {
                group.removeNode(node);
                blockToGroup.remove(node.pos);
            }

            processedGroups++;
            if (world.getTime() % 20 == 0 && processedGroups > 0) {
                NekoTechnology.LOGGER.debug("[导体管理器] 端口重新扫描进度: {}/{}", processedGroups, totalGroups);
            }
        }

        NekoTechnology.LOGGER.info("[导体管理器] 端口重新扫描完成: 处理了 {} 个导体组", totalGroups);

        // 保存到世界状态
        saveToWorldState();
    }


}

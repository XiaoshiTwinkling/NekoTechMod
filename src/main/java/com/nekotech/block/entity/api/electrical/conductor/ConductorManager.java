package com.nekotech.block.entity.api.electrical.conductor;

import com.nekotech.NekoTechnology;
import com.nekotech.block.entity.api.electrical.ITransferElectrical;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * 管理一个世界中的所有导体网络喵~
 * 那我是猫猫王了
 */
public class ConductorManager extends PersistentState {
    private static final Map<World, ConductorManager> INSTANCES = new java.util.concurrent.ConcurrentHashMap<>();

    // 方块位置 -> 所属导体组 的映射
    private final Map<BlockPos, ConductorGroup> blockToGroup = new HashMap<>();
    // 所有活跃的导体组
    private final Map<Integer, ConductorGroup> conductorGroups = new HashMap<>();
    private final PortScanner portScanner = new PortScanner();

    // 待重建的位置队列
    private final Set<BlockPos> pendingRebuildPositions = new HashSet<>();

    // 保存方块位置到导体组ID的映射
    private final Map<BlockPos, Integer> posToGroupId = new HashMap<>();

    //操作类型枚举
    private enum OperationType {
        PLACE, BREAK, COMPONENT_CHANGE
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



    // 操作缓存队列
    private final Queue<PendingOperation> operationQueue = new LinkedList<>();
    // 已处理的位置（用于去重）
    private final Set<BlockPos> processedPositions = new HashSet<>();

    public static ConductorManager get(World world) {
        if (world.isClient) {
            return null;
        }

        ServerWorld serverWorld = (ServerWorld) world;
        PersistentStateManager persistentStateManager = serverWorld.getPersistentStateManager();


        return INSTANCES.computeIfAbsent(world, w -> new ConductorManager());
    }

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

    private void processOperation(World world, PendingOperation operation) {
        if (processedPositions.contains(operation.pos)) {
            NekoTechnology.LOGGER.debug("[导体管理器] 跳过已处理的位置: {}", operation.pos);
            return;
        }

        switch (operation.type) {
            case PLACE -> handleBlockPlace(world, operation.pos);
            case BREAK -> handleBlockBreak(world, operation.pos);
            case COMPONENT_CHANGE -> handleComponentChange(world, operation.pos, operation.side);
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
    }

    private boolean isConductor(World world, BlockPos pos) {
        BlockEntity be = world.getBlockEntity(pos);
        return be instanceof ITransferElectrical;
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
        markDirty();
        NekoTechnology.LOGGER.info("[导体管理器] 注册导体组: {}", group);
    }

    private void removeGroup(ConductorGroup group) {
        conductorGroups.remove(group.id);
        for (ConductorNode node : group.nodes) {
            blockToGroup.remove(node.pos);
        }
        markDirty();
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

    public static ConductorManager fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        ConductorManager manager = new ConductorManager();
        manager.readNbt(nbt, registryLookup);
        return manager;
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
            // 遍历组中所有节点
            for (ConductorNode node : new HashSet<>(group.nodes)) {
                // 清除旧端口
                group.removePortsFromNode(node);
                node.inputPort = null;
                node.outputPort = null;

                // 重新扫描端口
                portScanner.scanPorts(world, node);

                // 更新端口映射
                group.updatePortsFromNode(node);
            }

            processedGroups++;
            if (world.getTime() % 20 == 0 && processedGroups > 0) {
                NekoTechnology.LOGGER.debug("[导体管理器] 端口重新扫描进度: {}/{}", processedGroups, totalGroups);
            }
        }

        NekoTechnology.LOGGER.info("[导体管理器] 端口重新扫描完成: 处理了 {} 个导体组", totalGroups);
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        // 保存当前的最大ID
        nbt.putInt("NextGroupId", getNextId());

        // 保存方块位置到导体组ID的映射
        NbtList posMapping = new NbtList();
        for (Map.Entry<BlockPos, Integer> entry : posToGroupId.entrySet()) {
            BlockPos pos = entry.getKey();
            NbtCompound entryNbt = new NbtCompound();
            entryNbt.putInt("X", pos.getX());
            entryNbt.putInt("Y", pos.getY());
            entryNbt.putInt("Z", pos.getZ());
            entryNbt.putInt("GroupId", entry.getValue());
            posMapping.add(entryNbt);
        }
        nbt.put("PosToGroupId", posMapping);

        // 保存所有导体组
        NbtList groupsList = new NbtList();
        for (ConductorGroup group : conductorGroups.values()) {
            NbtCompound groupNbt = new NbtCompound();
            writeGroupToNbt(group, groupNbt, registryLookup);
            groupsList.add(groupNbt);
        }
        nbt.put("ConductorGroups", groupsList);

        return nbt;
    }

    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        // 清空现有数据
        blockToGroup.clear();
        conductorGroups.clear();
        posToGroupId.clear();

        // 加载下一个ID
        if (nbt.contains("NextGroupId", NbtElement.INT_TYPE)) {
            int nextId = nbt.getInt("NextGroupId");
            setNextId(nextId);
            NekoTechnology.LOGGER.info("[导体管理器] 加载NextGroupId: {}", nextId);
        }

        // 加载方块位置到导体组ID的映射
        if (nbt.contains("PosToGroupId", NbtElement.LIST_TYPE)) {
            NbtList posMapping = nbt.getList("PosToGroupId", NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < posMapping.size(); i++) {
                NbtCompound entryNbt = posMapping.getCompound(i);
                int x = entryNbt.getInt("X");
                int y = entryNbt.getInt("Y");
                int z = entryNbt.getInt("Z");
                int groupId = entryNbt.getInt("GroupId");

                BlockPos pos = new BlockPos(x, y, z);
                posToGroupId.put(pos, groupId);
            }
        }

        // 加载所有导体组
        if (nbt.contains("ConductorGroups", NbtElement.LIST_TYPE)) {
            NbtList groupsList = nbt.getList("ConductorGroups", NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < groupsList.size(); i++) {
                NbtCompound groupNbt = groupsList.getCompound(i);
                ConductorGroup group = readGroupFromNbt(groupNbt, registryLookup);
                conductorGroups.put(group.id, group);
            }
        }

        // 重建方块位置到导体组的映射
        rebuildBlockToGroupMapping();

        // 设置需要重新扫描端口的标志
        this.needsRescan = true;
        NekoTechnology.LOGGER.info("[导体管理器] 加载完成: {} 个导体组, {} 个方块映射, 需要重新扫描: {}",
                conductorGroups.size(), blockToGroup.size(), needsRescan);
    }

    /**
     * 将导体组保存到NBT
     */
    private void writeGroupToNbt(ConductorGroup group, NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        nbt.putInt("Id", group.id);

        // 保存节点
        NbtList nodesList = new NbtList();
        for (ConductorNode node : group.nodes) {
            NbtCompound nodeNbt = new NbtCompound();

            // 保存位置
            nodeNbt.putInt("X", node.pos.getX());
            nodeNbt.putInt("Y", node.pos.getY());
            nodeNbt.putInt("Z", node.pos.getZ());

            // 保存连接
            NbtList connections = new NbtList();
            for (Direction dir : node.connections) {
                connections.add(NbtString.of(dir.getName()));
            }
            nodeNbt.put("Connections", connections);

            // 保存端口信息
            if (node.inputPort != null) {
                NbtCompound portNbt = new NbtCompound();
                portNbt.putString("Type", node.inputPort.type.name());
                portNbt.putFloat("MaxRate", node.inputPort.maxRate);
                portNbt.putFloat("Efficiency", node.inputPort.efficiency);
                portNbt.putInt("MachineX", node.inputPort.machinePos.getX());
                portNbt.putInt("MachineY", node.inputPort.machinePos.getY());
                portNbt.putInt("MachineZ", node.inputPort.machinePos.getZ());
                portNbt.putInt("PortX", node.inputPort.portPos.getX());
                portNbt.putInt("PortY", node.inputPort.portPos.getY());
                portNbt.putInt("PortZ", node.inputPort.portPos.getZ());
                portNbt.putBoolean("IsSelf", node.inputPort.isSelf);
                portNbt.putString("SourceItemId", node.inputPort.sourceItemId);
                nodeNbt.put("InputPort", portNbt);
            }

            if (node.outputPort != null) {
                NbtCompound portNbt = new NbtCompound();
                portNbt.putString("Type", node.outputPort.type.name());
                portNbt.putFloat("MaxRate", node.outputPort.maxRate);
                portNbt.putFloat("Efficiency", node.outputPort.efficiency);
                portNbt.putInt("MachineX", node.outputPort.machinePos.getX());
                portNbt.putInt("MachineY", node.outputPort.machinePos.getY());
                portNbt.putInt("MachineZ", node.outputPort.machinePos.getZ());
                portNbt.putInt("PortX", node.outputPort.portPos.getX());
                portNbt.putInt("PortY", node.outputPort.portPos.getY());
                portNbt.putInt("PortZ", node.outputPort.portPos.getZ());
                portNbt.putBoolean("IsSelf", node.outputPort.isSelf);
                portNbt.putString("SourceItemId", node.outputPort.sourceItemId);
                nodeNbt.put("OutputPort", portNbt);
            }

            nodesList.add(nodeNbt);
        }
        nbt.put("Nodes", nodesList);

        NekoTechnology.LOGGER.debug("[导体管理器] 保存导体组#{}, 节点数: {}", group.id, group.nodes.size());
    }

    /**
     * 从NBT加载导体组
     */
    private ConductorGroup readGroupFromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        int id = nbt.getInt("Id");

        // 创建导体组
        ConductorGroup group = new ConductorGroup();

        // 通过反射设置ID
        try {
            java.lang.reflect.Field idField = ConductorGroup.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(group, id);
        } catch (Exception e) {
            NekoTechnology.LOGGER.error("无法设置导体组ID: {}", e.getMessage());
        }

        // 加载节点
        if (nbt.contains("Nodes", NbtElement.LIST_TYPE)) {
            NbtList nodesList = nbt.getList("Nodes", NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < nodesList.size(); i++) {
                NbtCompound nodeNbt = nodesList.getCompound(i);

                // 读取位置
                int x = nodeNbt.getInt("X");
                int y = nodeNbt.getInt("Y");
                int z = nodeNbt.getInt("Z");
                BlockPos pos = new BlockPos(x, y, z);

                ConductorNode node = new ConductorNode(pos);

                // 读取连接
                if (nodeNbt.contains("Connections", NbtElement.LIST_TYPE)) {
                    NbtList connections = nodeNbt.getList("Connections", NbtElement.STRING_TYPE);
                    for (int j = 0; j < connections.size(); j++) {
                        String dirName = connections.getString(j);
                        try {
                            Direction dir = Direction.byName(dirName);
                            if (dir != null) {
                                node.connections.add(dir);
                            }
                        } catch (IllegalArgumentException e) {
                            NekoTechnology.LOGGER.warn("无法解析方向: {}", dirName);
                        }
                    }
                }

                // 读取输入端口
                if (nodeNbt.contains("InputPort", NbtElement.COMPOUND_TYPE)) {
                    NbtCompound portNbt = nodeNbt.getCompound("InputPort");
                    Port port = new Port(
                            Port.Type.valueOf(portNbt.getString("Type")),
                            portNbt.getFloat("MaxRate"),
                            portNbt.getFloat("Efficiency"),
                            new BlockPos(
                                    portNbt.getInt("MachineX"),
                                    portNbt.getInt("MachineY"),
                                    portNbt.getInt("MachineZ")
                            ),
                            portNbt.getBoolean("IsSelf"),
                            new BlockPos(
                                    portNbt.getInt("PortX"),
                                    portNbt.getInt("PortY"),
                                    portNbt.getInt("PortZ")
                            ),
                            portNbt.getString("SourceItemId")
                    );
                    node.inputPort = port;
                }

                // 读取输出端口
                if (nodeNbt.contains("OutputPort", NbtElement.COMPOUND_TYPE)) {
                    NbtCompound portNbt = nodeNbt.getCompound("OutputPort");
                    Port port = new Port(
                            Port.Type.valueOf(portNbt.getString("Type")),
                            portNbt.getFloat("MaxRate"),
                            portNbt.getFloat("Efficiency"),
                            new BlockPos(
                                    portNbt.getInt("MachineX"),
                                    portNbt.getInt("MachineY"),
                                    portNbt.getInt("MachineZ")
                            ),
                            portNbt.getBoolean("IsSelf"),
                            new BlockPos(
                                    portNbt.getInt("PortX"),
                                    portNbt.getInt("PortY"),
                                    portNbt.getInt("PortZ")
                            ),
                            portNbt.getString("SourceItemId")
                    );
                    node.outputPort = port;
                }

                group.nodes.add(node);
            }
        }

        // 重新计算端口列表
        for (ConductorNode node : group.nodes) {
            if (node.inputPort != null) {
                group.inputPorts.add(node.inputPort);
            }
            if (node.outputPort != null) {
                group.outputPorts.add(node.outputPort);
            }
        }

        NekoTechnology.LOGGER.info("[导体管理器] 加载导体组#{}, 节点: {}, 输入: {}, 输出: {}",
                group.id, group.nodes.size(), group.inputPorts.size(), group.outputPorts.size());

        return group;
    }

    /**
     * 重建方块位置到导体组的映射
     */
    private void rebuildBlockToGroupMapping() {
        blockToGroup.clear();

        for (ConductorGroup group : conductorGroups.values()) {
            for (ConductorNode node : group.nodes) {
                blockToGroup.put(node.pos, group);
                posToGroupId.put(node.pos, group.id);
            }
        }
    }

    /**
     * 标记数据为脏（需要保存）
     */
    public void markDirty() {
        this.setDirty(true);
    }
}

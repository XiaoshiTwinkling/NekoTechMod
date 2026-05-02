package com.nekotech.block.entity.api.electrical.conductor;

import com.nekotech.NekoTechnology;
import com.nekotech.block.entity.api.electrical.ITransferElectrical;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
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

    public static ConductorManager get(World world) {
        if (world.isClient) {
            return null;
        }

        ServerWorld serverWorld = (ServerWorld) world;
        PersistentStateManager persistentStateManager = serverWorld.getPersistentStateManager();


        return INSTANCES.computeIfAbsent(world, w -> new ConductorManager());
    }

    /**
     * 当导体方块被放置时调用
     */
    public void onBlockPlaced(World world, BlockPos pos) {
        NekoTechnology.LOGGER.info("[导体管理器] 方块放置: {}", pos);

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

    /**
     * 当导体方块被破坏时调用
     */
    public void onBlockBroken(World world, BlockPos pos) {
        NekoTechnology.LOGGER.info("[导体管理器] 方块破坏: {}", pos);

        ConductorGroup group = blockToGroup.get(pos);
        if (group == null) {
            return;
        }

        // 检查是否需要分割导体组
        List<ConductorGroup> splitGroups = group.splitAt(world, pos);
        blockToGroup.remove(pos);

        if (splitGroups.isEmpty()) {
            // 导体组被完全移除
            removeGroup(group);
        } else if (splitGroups.size() == 1) {
            // 导体组没有分割，只是移除了一个节点
            ConductorGroup remaining = splitGroups.get(0);
            updateGroupMapping(remaining);
        } else {
            // 导体组被分割成多个
            removeGroup(group);
            for (ConductorGroup newGroup : splitGroups) {
                registerGroup(newGroup);
            }
        }
    }

    /**
     * 当零件变更时调用
     */
    public void onComponentChanged(World world, BlockPos pos, Direction side) {
        NekoTechnology.LOGGER.info("[导体管理器] 零件变更: {} {}", pos, side);

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

    public void tick(World world) {
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

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        // 保存方块位置到导体组ID的映射
        NbtCompound posMapping = new NbtCompound();
        for (Map.Entry<BlockPos, Integer> entry : posToGroupId.entrySet()) {
            String posKey = entry.getKey().getX() + "," + entry.getKey().getY() + "," + entry.getKey().getZ();
            posMapping.putInt(posKey, entry.getValue());
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

        NekoTechnology.LOGGER.info("[导体管理器] 保存了 {} 个导体组，{} 个方块映射",
                conductorGroups.size(), posToGroupId.size());

        return nbt;
    }

    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        // 清空现有数据
        blockToGroup.clear();
        conductorGroups.clear();
        posToGroupId.clear();

        // 加载方块位置到导体组ID的映射
        if (nbt.contains("PosToGroupId", NbtElement.COMPOUND_TYPE)) {
            NbtCompound posMapping = nbt.getCompound("PosToGroupId");
            for (String posKey : posMapping.getKeys()) {
                String[] parts = posKey.split(",");
                if (parts.length == 3) {
                    try {
                        int x = Integer.parseInt(parts[0]);
                        int y = Integer.parseInt(parts[1]);
                        int z = Integer.parseInt(parts[2]);
                        int groupId = posMapping.getInt(posKey);

                        BlockPos pos = new BlockPos(x, y, z);
                        posToGroupId.put(pos, groupId);
                    } catch (NumberFormatException e) {
                        NekoTechnology.LOGGER.warn("无法解析位置键: {}", posKey);
                    }
                }
            }
        }

        // 加载所有导体组
        if (nbt.contains("ConductorGroups", NbtElement.LIST_TYPE)) {
            NbtList groupsList = nbt.getList("ConductorGroups", NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < groupsList.size(); i++) {
                NbtCompound groupNbt = groupsList.getCompound(i);
                ConductorGroup group = readGroupFromNbt(groupNbt, registryLookup);
                if (group != null) {
                    conductorGroups.put(group.id, group);
                }
            }
        }

        // 重建方块位置到导体组的映射
        rebuildBlockToGroupMapping();

        NekoTechnology.LOGGER.info("[导体管理器] 加载了 {} 个导体组，{} 个方块映射",
                conductorGroups.size(), posToGroupId.size());
    }

    /**
     * 将导体组保存到NBT
     */
    private void writeGroupToNbt(ConductorGroup group, NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        nbt.putInt("Id", group.id);

        // 保存节点位置
        NbtList nodesList = new NbtList();
        for (ConductorNode node : group.nodes) {
            NbtCompound nodeNbt = new NbtCompound();
            nodeNbt.putInt("X", node.pos.getX());
            nodeNbt.putInt("Y", node.pos.getY());
            nodeNbt.putInt("Z", node.pos.getZ());
            nodesList.add(nodeNbt);
        }
        nbt.put("Nodes", nodesList);

        // 保存端口信息
        NbtList inputPortsList = new NbtList();
        for (Port port : group.inputPorts) {
            NbtCompound portNbt = writePortToNbt(port);
            inputPortsList.add(portNbt);
        }
        nbt.put("InputPorts", inputPortsList);

        NbtList outputPortsList = new NbtList();
        for (Port port : group.outputPorts) {
            NbtCompound portNbt = writePortToNbt(port);
            outputPortsList.add(portNbt);
        }
        nbt.put("OutputPorts", outputPortsList);
    }

    /**
     * 从NBT加载导体组
     */
    private ConductorGroup readGroupFromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        int id = nbt.getInt("Id");

        // 由于ConductorGroup的id是自动生成的，我们需要创建一个占位符
        // 在实际实现中，可能需要修改ConductorGroup的构造函数以支持指定ID
        ConductorGroup group = new ConductorGroup();

        // 使用反射或其他方法设置ID（这里简化处理）
        // 注意：实际实现中需要更优雅的方式处理ID

        // 加载节点
        if (nbt.contains("Nodes", NbtElement.LIST_TYPE)) {
            NbtList nodesList = nbt.getList("Nodes", NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < nodesList.size(); i++) {
                NbtCompound nodeNbt = nodesList.getCompound(i);
                int x = nodeNbt.getInt("X");
                int y = nodeNbt.getInt("Y");
                int z = nodeNbt.getInt("Z");
                BlockPos pos = new BlockPos(x, y, z);

                ConductorNode node = new ConductorNode(pos);
                group.addNode(node);
            }
        }

        return group;
    }

    /**
     * 将端口保存到NBT
     */
    private NbtCompound writePortToNbt(Port port) {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("Type", port.type.name());
        nbt.putFloat("MaxRate", port.maxRate);
        nbt.putFloat("Efficiency", port.efficiency);
        nbt.putInt("MachineX", port.machinePos.getX());
        nbt.putInt("MachineY", port.machinePos.getY());
        nbt.putInt("MachineZ", port.machinePos.getZ());
        nbt.putInt("PortX", port.portPos.getX());
        nbt.putInt("PortY", port.portPos.getY());
        nbt.putInt("PortZ", port.portPos.getZ());
        nbt.putBoolean("IsSelf", port.isSelf);
        nbt.putString("SourceItemId", port.sourceItemId);
        return nbt;
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

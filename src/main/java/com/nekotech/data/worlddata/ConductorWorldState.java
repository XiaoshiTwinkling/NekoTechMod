package com.nekotech.data.worlddata;

import com.nekotech.block.entity.api.electrical.conductor.Port;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

import java.util.*;

public class ConductorWorldState extends PersistentState {
    private static final String ID = "conductor_world_state";

    private static final String NEXT_GROUP_ID = "next_group_id";
    private static final String GROUPS = "groups";

    private int nextGroupId = 0;
    private final Map<Integer, ConductorGroupData> groups = new HashMap<>();

    public int getNextGroupId() { return nextGroupId; }
    public void setNextGroupId(int id) {
        this.nextGroupId = id;
        this.markDirty();
    }
    public Map<Integer, ConductorGroupData> getGroups() { return groups; }

    public static final PersistentState.Type<ConductorWorldState> TYPE =
            new PersistentState.Type<>(
                    ConductorWorldState::new,
                    ConductorWorldState::fromNbt,
                    DataFixTypes.LEVEL
            );


    public static ConductorWorldState get(MinecraftServer server) {
        ServerWorld overworld = server.getWorld(World.OVERWORLD);

        return overworld.getPersistentStateManager().getOrCreate(TYPE, ID);
    }

    /**
     * 静态工厂方法，用于从NBT数据重建状态。
     * 由 PersistentState.Type 调用。
     */
    public static ConductorWorldState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        ConductorWorldState state = new ConductorWorldState();
        state.readNbt(nbt, registryLookup);
        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound rootNbt, RegistryWrapper.WrapperLookup registryLookup) {

        rootNbt.putInt(NEXT_GROUP_ID, nextGroupId);

        NbtList groupsList = new NbtList();
        for (ConductorGroupData group : groups.values()) {
            NbtCompound groupNbt = new NbtCompound();
            writeGroupToNbt(group, groupNbt, registryLookup);
            groupsList.add(groupNbt);
        }
        rootNbt.put(GROUPS, groupsList);

        return rootNbt;
    }

    private void writeGroupToNbt(ConductorGroupData group, NbtCompound groupNbt, RegistryWrapper.WrapperLookup registryLookup) {
        groupNbt.putInt("id", group.id);

        // 保存节点列表
        NbtList nodesList = new NbtList();
        for (ConductorNodeData node : group.nodes) {
            NbtCompound nodeNbt = new NbtCompound();
            writeNodeToNbt(node, nodeNbt, registryLookup);
            nodesList.add(nodeNbt);
        }
        groupNbt.put("nodes", nodesList);
    }

    /**
     * 将一个节点数据写入NBT。
     */
    private void writeNodeToNbt(ConductorNodeData node, NbtCompound nodeNbt, RegistryWrapper.WrapperLookup registryLookup) {
        // 保存位置
        nodeNbt.putInt("x", node.pos.getX());
        nodeNbt.putInt("y", node.pos.getY());
        nodeNbt.putInt("z", node.pos.getZ());

        // 保存连接方向
        NbtList connectionsList = new NbtList();
        for (Direction dir : node.connections) {
            connectionsList.add(NbtString.of(dir.getName()));
        }
        nodeNbt.put("connections", connectionsList);

        // 保存输入端口
        if (node.inputPort != null) {
            NbtCompound portNbt = new NbtCompound();
            writePortToNbt(node.inputPort, portNbt);
            nodeNbt.put("input_port", portNbt);
        }

        // 保存输出端口
        if (node.outputPort != null) {
            NbtCompound portNbt = new NbtCompound();
            writePortToNbt(node.outputPort, portNbt);
            nodeNbt.put("output_port", portNbt);
        }
    }

    /**
     * 将端口数据写入NBT。
     */
    private void writePortToNbt(PortData port, NbtCompound portNbt) {
        portNbt.putString("type", port.type.name());
        portNbt.putFloat("max_rate", port.maxRate);
        portNbt.putFloat("efficiency", port.efficiency);
        portNbt.putInt("machine_x", port.machinePos.getX());
        portNbt.putInt("machine_y", port.machinePos.getY());
        portNbt.putInt("machine_z", port.machinePos.getZ());
        portNbt.putBoolean("is_self", port.isSelf);
        portNbt.putString("source_item", port.sourceItemId);
    }

    // --- 数据反序列化（从硬盘加载） ---
    public void readNbt(NbtCompound rootNbt, RegistryWrapper.WrapperLookup registryLookup) {
        // 清空现有数据
        groups.clear();

        // 1. 加载下一个可用的组ID
        if (rootNbt.contains(NEXT_GROUP_ID, NbtElement.INT_TYPE)) {
            nextGroupId = rootNbt.getInt(NEXT_GROUP_ID);
        }

        // 2. 加载所有导体组
        if (rootNbt.contains(GROUPS, NbtElement.LIST_TYPE)) {
            NbtList groupsList = rootNbt.getList(GROUPS, NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < groupsList.size(); i++) {
                NbtCompound groupNbt = groupsList.getCompound(i);
                ConductorGroupData group = readGroupFromNbt(groupNbt, registryLookup);
                if (group != null) {
                    groups.put(group.id, group);
                }
            }
        }

    }

    private ConductorGroupData readGroupFromNbt(NbtCompound groupNbt, RegistryWrapper.WrapperLookup registryLookup) {
        if (!groupNbt.contains("id", NbtElement.INT_TYPE)) {
            return null;
        }
        int id = groupNbt.getInt("id");
        ConductorGroupData group = new ConductorGroupData(id);

        if (groupNbt.contains("nodes", NbtElement.LIST_TYPE)) {
            NbtList nodesList = groupNbt.getList("nodes", NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < nodesList.size(); i++) {
                NbtCompound nodeNbt = nodesList.getCompound(i);
                ConductorNodeData node = readNodeFromNbt(nodeNbt, registryLookup);
                if (node != null) {
                    group.nodes.add(node);
                }
            }
        }
        return group;
    }

    private ConductorNodeData readNodeFromNbt(NbtCompound nodeNbt, RegistryWrapper.WrapperLookup registryLookup) {
        if (!nodeNbt.contains("x", NbtElement.INT_TYPE) ||
                !nodeNbt.contains("y", NbtElement.INT_TYPE) ||
                !nodeNbt.contains("z", NbtElement.INT_TYPE)) {
            return null;
        }
        BlockPos pos = new BlockPos(
                nodeNbt.getInt("x"),
                nodeNbt.getInt("y"),
                nodeNbt.getInt("z")
        );
        ConductorNodeData node = new ConductorNodeData(pos);

        // 读取连接
        if (nodeNbt.contains("connections", NbtElement.LIST_TYPE)) {
            NbtList connections = nodeNbt.getList("connections", NbtElement.STRING_TYPE);
            for (int i = 0; i < connections.size(); i++) {
                String dirName = connections.getString(i);
                try {
                    Direction dir = Direction.byName(dirName);
                    if (dir != null) {
                        node.connections.add(dir);
                    }
                } catch (IllegalArgumentException ignored) {}
            }
        }

        // 读取输入端口
        if (nodeNbt.contains("input_port", NbtElement.COMPOUND_TYPE)) {
            node.inputPort = readPortFromNbt(nodeNbt.getCompound("input_port"));
        }

        // 读取输出端口
        if (nodeNbt.contains("output_port", NbtElement.COMPOUND_TYPE)) {
            node.outputPort = readPortFromNbt(nodeNbt.getCompound("output_port"));
        }

        return node;
    }

    private PortData readPortFromNbt(NbtCompound portNbt) {
        try {
            Port.Type type = Port.Type.valueOf(portNbt.getString("type"));
            float maxRate = portNbt.getFloat("max_rate");
            float efficiency = portNbt.getFloat("efficiency");
            BlockPos machinePos = new BlockPos(
                    portNbt.getInt("machine_x"),
                    portNbt.getInt("machine_y"),
                    portNbt.getInt("machine_z")
            );
            boolean isSelf = portNbt.getBoolean("is_self");
            String sourceItemId = portNbt.getString("source_item");
            return new PortData(type, maxRate, efficiency, machinePos, isSelf, sourceItemId);
        } catch (Exception e) {
            return null;
        }
    }

    public static class ConductorGroupData {
        public final int id;
        public final List<ConductorNodeData> nodes = new ArrayList<>();

        public ConductorGroupData(int id) {
            this.id = id;
        }
    }

    public static class ConductorNodeData {
        public final BlockPos pos;
        public final Set<Direction> connections = new HashSet<>();
        public PortData inputPort;
        public PortData outputPort;

        public ConductorNodeData(BlockPos pos) {
            this.pos = pos;
        }
    }

    public static class PortData {
        public final Port.Type type;
        public final float maxRate;
        public final float efficiency;
        public final BlockPos machinePos;
        public final boolean isSelf;
        public final String sourceItemId;

        public PortData(Port.Type type, float maxRate, float efficiency,
                        BlockPos machinePos, boolean isSelf, String sourceItemId) {
            this.type = type;
            this.maxRate = maxRate;
            this.efficiency = efficiency;
            this.machinePos = machinePos;
            this.isSelf = isSelf;
            this.sourceItemId = sourceItemId;
        }
    }

    /**
     * 以下与接线柱相关
     */
    public static class WirePairData {
        public BlockPos pos1;
        public Direction side1;
        public BlockPos pos2;
        public Direction side2;
        public String wireType;

        public WirePairData(BlockPos pos1, Direction side1, BlockPos pos2, Direction side2, String wireType) {
            this.pos1 = pos1;
            this.side1 = side1;
            this.pos2 = pos2;
            this.side2 = side2;
            this.wireType = wireType;
        }
    }

    private final Map<String, WirePairData> wirePairs = new HashMap<>();

    public Map<String, WirePairData> getWirePairs() {
        return wirePairs;
    }

    // 添加或更新配对
    public void addWirePair(String key, WirePairData data) {
        wirePairs.put(key, data);
        markDirty();
    }

    // 移除配对
    public void removeWirePair(String key) {
        wirePairs.remove(key);
        markDirty();
    }

    // 生成唯一键
    public static String generateWirePairKey(BlockPos pos, Direction side) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ() + "_" + side.getName();
    }
}

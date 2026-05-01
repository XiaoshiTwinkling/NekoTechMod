package com.nekotech.block.entity.api.electrical;

import com.nekotech.NekoTechnology;
import com.nekotech.block.entity.api.component.ComponentAdaptation;
import com.nekotech.item.ModItems;
import com.nekotech.item.custom.component.FluxInputerItem;
import com.nekotech.item.custom.component.FluxOutputerItem;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;

import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.*;

/**
 * 这个类代表一个独立的导体网络
 *
 * 实现了接口ITransferElectrical的方块可以传输电力 而互相紧挨着的一堆实现了这个接口的方块可以看做一整个导体 而这一整个导体具有多个输入输出口：
 * 当某个方块使用了flux_input零件或者被flux_output零件对准时 这个方块会被这个导体标记为导体的输出口
 * 当某个方块使用了flux_output零件或者被flux_input零件对准时 这个方块会被这个导体标记为导体的输入口
 * 传输到输出口的效率取决于输入输出零件的效率
 * 当导体的某个方块发生变化 就会重新计算导体
 */
public class Conductor {
    // 导体网络唯一ID，用于调试
    private final int id;
    private static int nextId = 0;

    // 导体网络包含的所有导体方块坐标
    private final Set<BlockPos> blocks = new HashSet<>();
    // 导体网络的输入端口 (能量从此进入网络)
    private final List<Port> inputPorts = new ArrayList<>();
    // 导体网络的输出端口 (能量从此离开网络)
    private final List<Port> outputPorts = new ArrayList<>();

    // 端口类型枚举
    public enum PortType { INPUT, OUTPUT }

    /**
     * 导体端口
     */
    public static class Port {
        public final BlockPos pos;          // 端口所在方块位置
        public final PortType type;         // 端口类型
        public final Direction side;        // 端口方向 (零件安装面或能量来源方向)
        public final float efficiency;      // 连接到此端口的零件效率
        public final String sourceItemId;   // 来源物品ID，用于调试

        public Port(BlockPos pos, PortType type, Direction side, float efficiency, String sourceItemId) {
            this.pos = pos;
            this.type = type;
            this.side = side;
            this.efficiency = efficiency;
            this.sourceItemId = sourceItemId;
        }

        @Override
        public String toString() {
            return String.format("Port[%s@%s side=%s eff=%.1f]", type, pos, side, efficiency);
        }
    }

    public Conductor() {
        this.id = nextId++;
        NekoTechnology.LOGGER.info("[导体网络] 创建新网络 #{}", id);
    }

    /**
     * 获取导体网络ID
     */
    public int getId() {
        return id;
    }

    /**
     * 获取导体网络中所有方块
     */
    public Set<BlockPos> getBlocks() {
        return Collections.unmodifiableSet(blocks);
    }

    /**
     * 获取输入端口列表
     */
    public List<Port> getInputPorts() {
        return Collections.unmodifiableList(inputPorts);
    }

    /**
     * 获取输出端口列表
     */
    public List<Port> getOutputPorts() {
        return Collections.unmodifiableList(outputPorts);
    }

    /**
     * 构建或重建此网络。从给定的起始方块开始，BFS搜索实现了ITransferElectrical的方块 然后全部丢到导体网络中
     * 收集所有相邻的导体方块，并识别端口。
     *
     * @param world 世界实例
     * @param startPos 起始搜索位置 (通常是最新变化的方块)
     */
    public void rebuild(World world, BlockPos startPos) {
        blocks.clear();
        inputPorts.clear();
        outputPorts.clear();

        if (world == null || !(world.getBlockEntity(startPos) instanceof ITransferElectrical)) {
            return;
        }

        // BFS 收集所有相连的导体方块
        Queue<BlockPos> queue = new LinkedList<>();
        Set<BlockPos> visited = new HashSet<>();

        queue.offer(startPos);
        visited.add(startPos);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            blocks.add(current);

            // 检查六个相邻方向
            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = current.offset(dir);

                if (visited.contains(neighborPos)) {
                    continue;
                }

                BlockEntity neighborEntity = world.getBlockEntity(neighborPos);
                if (neighborEntity instanceof ITransferElectrical) {
                    // 找到新的导体方块，加入搜索队列
                    queue.offer(neighborPos);
                    visited.add(neighborPos);
                }
            }
        }
        // 识别所有端口
        identifyPorts(world);
    }

    /**
     * 识别网络中的所有输入/输出端口
     */
    private void identifyPorts(World world) {
        for (BlockPos pos : blocks) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof ComponentAdaptation ca) {
                // 检查此方块上安装的零件
                identifyPortsOnBlock(world, pos, ca, true);
            }
            // 检查相邻方块对准此方块的零件
            identifyPortsFromNeighbors(world, pos);
        }
    }

    /**
     * 检查指定方块自身安装的零件，确定其是否为端口
     * @param isSelf true:检查自己身上的零件; false:检查邻居对准自己的零件(递归调用时用)
     */
    private void identifyPortsOnBlock(World world, BlockPos pos, ComponentAdaptation machine, boolean isSelf) {
        Map<Direction, net.minecraft.item.Item> attached = machine.getAttachedComponents();
        for (Map.Entry<Direction, net.minecraft.item.Item> entry : attached.entrySet()) {
            Direction side = entry.getKey();
            net.minecraft.item.Item item = entry.getValue();

            if (item instanceof FluxInputerItem inputer) {
                // 此方块安装了 flux_input 零件
                Port port = new Port(pos,
                        isSelf ? PortType.INPUT : PortType.OUTPUT, // 自身输入零件是输入口，邻居的输入零件对准我，则我是输出口
                        side,
                        inputer.getInputSpeed(),
                        "flux_inputer");
                if (isSelf) {
                    inputPorts.add(port);
                } else {
                    outputPorts.add(port);
                }
            } else if (item instanceof FluxOutputerItem outputer) {
                // 此方块安装了 flux_output 零件
                Port port = new Port(pos,
                        isSelf ? PortType.OUTPUT : PortType.INPUT, // 自身输出零件是输出口，邻居的输出零件对准我，则我是输入口
                        side,
                        outputer.getOutputSpeed(),
                        "flux_outputer");
                if (isSelf) {
                    outputPorts.add(port);
                } else {
                    inputPorts.add(port);
                }
            }
        }
    }

    /**
     * 检查六个相邻方块，看是否有零件对准当前方块，从而将当前方块定义为端口
     */
    private void identifyPortsFromNeighbors(World world, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.offset(dir);
            BlockEntity neighborEntity = world.getBlockEntity(neighborPos);

            if (neighborEntity instanceof ComponentAdaptation neighborMachine) {
                // 获取邻居在此方向安装的零件
                net.minecraft.item.Item item = neighborMachine.getComponent(dir);
                if (item != null) {
                    // 此邻居在此面安装了零件，且此面对准了当前方块
                    // 模拟一次检查，但 isSelf 设为 false
                    Map<Direction, net.minecraft.item.Item> singleEntryMap = Collections.singletonMap(dir, item);
                    // 临时包装以便复用识别逻辑
                    ComponentAdaptation temp = new ComponentAdaptation() {
                        @Override
                        public Map<Direction, net.minecraft.item.Item> getAttachedComponents() {
                            return singleEntryMap;
                        }
                        @Override
                        public Set<Item> getValidComponents() {
                            return ModItems.getAllComponents();
                        }
                    };
                    identifyPortsOnBlock(world, pos, temp, false);
                }
            }
        }
    }

    /**
     * 网络的更新
     */
    public void tick(World world) {
        if (world.isClient || blocks.isEmpty()) {
            return;
        }

        // 从所有输入端口收集能量
        float totalInputThisTick = collectInputFromPorts(world);
        if (totalInputThisTick <= 0.0f) {
            return; // 本 tick 无能量输入
        }

        // 计算所有输出端口的需求
        OutputDemandResult demandResult = calculateOutputDemands(world);
        float totalDemand = demandResult.totalDemand;
        List<PortAllocation> allocations = demandResult.allocations;

        if (totalDemand <= 0.0f || allocations.isEmpty()) {
            return; // 无输出需求
        }

        // 分配能量
        float energyToDistribute = Math.min(totalInputThisTick, totalDemand);
        distributeEnergyToOutputs(world, energyToDistribute, allocations);
    }

    /**
     * 从所有输入端口收集能量
     * @return 本tick可从所有输入端口获取的总能量
     */
    private float collectInputFromPorts(World world) {
        float total = 0.0f;
        for (Port inputPort : inputPorts) {
            float extracted = extractEnergyFromSource(world, inputPort);
            total += extracted;
        }
        return total;
    }

    /**
     * 从一个具体地输入端口提取能量
     */
    private float extractEnergyFromSource(World world, Port inputPort) {
        // 根据端口类型，确定能量来源
        BlockEntity sourceEntity = getEnergySourceEntity(world, inputPort);
        if (!(sourceEntity instanceof IElectricalMachine sourceMachine)) {
            return 0.0f; // 来源不是电力机器
        }

        // 计算基于零件效率的本tick可提取量
        float availableFromSource = sourceMachine.getNekoFlux();
        float maxExtractable = inputPort.efficiency;
        float toExtract = Math.min(availableFromSource, maxExtractable);

        if (toExtract > 0) {
            sourceMachine.receiveFlux(toExtract); // 从源机器扣除
            sourceEntity.markDirty();
        }
        return toExtract;
    }

    /**
     * 根据端口信息，找到提供能量的方块实体
     */
    private BlockEntity getEnergySourceEntity(World world, Port port) {
        // 端口类型决定了能量流动方向
        if (port.type == PortType.INPUT) {
            // 输入口：能量流入此端口所在的方块。
            // 如果此端口是由自身安装的 flux_input 零件产生，则源是此方块自身。
            // 如果此端口是由邻居的 flux_output 零件对准产生，则源是邻居方块。
            // 我们需要通过端口的方向 (port.side) 来判断。
            // 简化处理：假设端口数据中的 side 总是指向能量来源的方向。
            // 对于“被邻居对准”形成的输入口，side 应指向邻居。来源是 world.getBlockEntity(pos.offset(side))
            // 对于“自身安装零件”形成的输入口，side 是零件安装面，但能量来源于自身。我们约定此时传入 side=null 或自身？
            // 这里需与端口识别逻辑保持一致。为简化，我们假设：
            //   - 如果 side == null，能量来源于自身方块 (pos)
            //   - 否则，能量来源于相邻方块 (pos.offset(side))
            if (port.side == null) {
                return world.getBlockEntity(port.pos);
            } else {
                return world.getBlockEntity(port.pos.offset(port.side));
            }
        } else {
            // 对于输出口，在收集输入时不应被调用。这里返回null。
            return null;
        }
    }

    /**
     * 封装输出端口需求计算结果
     */
    private static class OutputDemandResult {
        final float totalDemand;
        final List<PortAllocation> allocations;

        OutputDemandResult(float totalDemand, List<PortAllocation> allocations) {
            this.totalDemand = totalDemand;
            this.allocations = allocations;
        }
    }

    /**
     * 封装单个输出端口的分配信息
     */
    private static class PortAllocation {
        final Port port;
        final float demand;           // 该端口的需求量
        float allocated;              // 实际分配到的能量（初始为0）

        PortAllocation(Port port, float demand) {
            this.port = port;
            this.demand = demand;
            this.allocated = 0.0f;
        }
    }

    /**
     * 计算所有输出端口的总需求和每个端口的具体需求
     */
    private OutputDemandResult calculateOutputDemands(World world) {
        float totalDemand = 0.0f;
        List<PortAllocation> allocations = new ArrayList<>();

        for (Port outputPort : outputPorts) {
            float demand = calculateSingleOutputDemand(world, outputPort);
            if (demand > 0) {
                totalDemand += demand;
                allocations.add(new PortAllocation(outputPort, demand));
            }
        }
        return new OutputDemandResult(totalDemand, allocations);
    }

    /**
     * 计算单个输出端口的需求量
     */
    private float calculateSingleOutputDemand(World world, Port outputPort) {
        // 找到接收能量的目标机器
        BlockEntity targetEntity = getEnergyTargetEntity(world, outputPort);
        if (!(targetEntity instanceof IElectricalMachine targetMachine)) {
            return 0.0f;
        }

        // 目标机器还能接收多少能量？
        float spaceAvailable = targetMachine.getMaxNekoFlux() - targetMachine.getNekoFlux();
        if (spaceAvailable <= 0) {
            return 0.0f; // 已满
        }

        // 本tick最多能传入多少（受零件效率限制）
        float maxThroughput = outputPort.efficiency; // 效率值作为能量/tick速率
        return Math.min(spaceAvailable, maxThroughput);
    }

    /**
     * 根据端口信息，找到接收能量的方块实体
     */
    private BlockEntity getEnergyTargetEntity(World world, Port port) {
        // 逻辑与 getEnergySourceEntity 对称但相反
        if (port.type == PortType.OUTPUT) {
            if (port.side == null) {
                return world.getBlockEntity(port.pos); // 能量输出到自身方块
            } else {
                return world.getBlockEntity(port.pos.offset(port.side)); // 能量输出到相邻方块
            }
        } else {
            return null;
        }
    }

    /**
     * 将总能量按需求比例分配给各个输出端口
     */
    private void distributeEnergyToOutputs(World world, float totalEnergy, List<PortAllocation> allocations) {
        if (totalEnergy <= 0 || allocations.isEmpty()) {
            return;
        }

        // 按需求比例分配
        float remainingEnergy = totalEnergy;
        for (PortAllocation alloc : allocations) {
            float share = (float) ((alloc.demand / allocations.stream().mapToDouble(a -> a.demand).sum()) * totalEnergy);
            alloc.allocated = share;
        }

        // 执行实际能量添加
        for (PortAllocation alloc : allocations) {
            if (alloc.allocated <= 0) continue;

            BlockEntity targetEntity = getEnergyTargetEntity(world, alloc.port);
            if (targetEntity instanceof IElectricalMachine targetMachine) {
                targetMachine.addFlux(alloc.allocated);
                targetEntity.markDirty();
            }
        }
    }
}

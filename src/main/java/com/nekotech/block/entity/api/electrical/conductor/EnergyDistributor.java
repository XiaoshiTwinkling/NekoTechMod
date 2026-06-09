package com.nekotech.block.entity.api.electrical.conductor;

import com.nekotech.NekoTechnology;
import com.nekotech.block.entity.api.electrical.IElectricalAppliance;
import com.nekotech.block.entity.api.electrical.IElectricalMachine;
import com.nekotech.block.entity.api.electrical.IGenerator;
import com.nekotech.block.entity.api.electrical.IElectricStorager;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.world.World;

import java.util.*;


public class EnergyDistributor {

    public static class Allocation {
        public final Port sourcePort;   // 能量来源（发电机/储电器输出）
        public final Port sinkPort;     // 能量去向（用电器/储电器输入）
        public final float amount;

        public Allocation(Port sourcePort, Port sinkPort, float amount) {
            this.sourcePort = sourcePort;
            this.sinkPort = sinkPort;
            this.amount = amount;
        }
    }

    public static class AllocationResult {
        public final List<Allocation> allocations = new ArrayList<>();
        public final float totalTransferred;

        public AllocationResult(List<Allocation> allocations, float totalTransferred) {
            this.allocations.addAll(allocations);
            this.totalTransferred = totalTransferred;
        }
    }

    public AllocationResult distributeEnergy(World world, ConductorGroup group) {
        // 1. 收集所有端口，并按机器类型分类
        List<SourceInfo> generatorSources = new ArrayList<>();
        List<SourceInfo> storageSources = new ArrayList<>();
        List<SinkInfo> applianceSinks = new ArrayList<>();
        List<SinkInfo> storageSinks = new ArrayList<>();

        // 收集输入端口（能量来源：机器→网络）
        for (Port port : group.inputPorts) {
            BlockEntity be = world.getBlockEntity(port.machinePos);
            if (be instanceof IElectricalMachine machine) {
                float available = machine.getNekoFlux();
                float maxExtractable = Math.min(available, port.getEffectiveRate());
                if (maxExtractable <= 0.001f) continue;

                SourceInfo info = new SourceInfo(port, machine, maxExtractable);
                if (be instanceof IGenerator) {
                    generatorSources.add(info);
                } else if (be instanceof IElectricStorager) {
                    storageSources.add(info);
                } else {
                    storageSources.add(info);
                }
            }
        }

        for (Port port : group.outputPorts) {
            BlockEntity be = world.getBlockEntity(port.machinePos);
            if (be instanceof IElectricalMachine machine) {
                float space = machine.getMaxNekoFlux() - machine.getNekoFlux();
                float maxAcceptable = Math.min(space, port.getEffectiveRate());
                if (maxAcceptable <= 0.001f) continue;

                SinkInfo info = new SinkInfo(port, machine, maxAcceptable);
                if (be instanceof IElectricalAppliance) {
                    applianceSinks.add(info);
                } else if (be instanceof IElectricStorager) {
                    storageSinks.add(info);
                } else {
                    // 其他类型
                    storageSinks.add(info);
                }
            }
        }

        if (generatorSources.isEmpty() && storageSources.isEmpty()) {
            return new AllocationResult(Collections.emptyList(), 0);
        }
        if (applianceSinks.isEmpty() && storageSinks.isEmpty()) {
            return new AllocationResult(Collections.emptyList(), 0);
        }

        List<Allocation> allocations = new ArrayList<>();

        // 发电机 → 用电器
        allocateBetween(generatorSources, applianceSinks, allocations);

        // 储电器（放电） → 用电器（补充剩余需求）
        allocateBetween(storageSources, applianceSinks, allocations);

        // 发电机（剩余） → 储电器（充电）
        allocateBetween(generatorSources, storageSinks, allocations);

        if (!allocations.isEmpty()) {
            executeAllocations(world, allocations);
        }

        float totalTransferred = allocations.stream().map(a -> a.amount).reduce(0f, Float::sum);
        if (totalTransferred > 0 && world.getTime() % 200 == 0) {
            NekoTechnology.LOGGER.info("[能量分配器] 传输: {:.2f} NF, 分配项数: {}", totalTransferred, allocations.size());
        }

        return new AllocationResult(allocations, totalTransferred);
    }

    /**
     * 在两个集合之间按比例分配能量
     */
    private void allocateBetween(List<SourceInfo> sources, List<SinkInfo> sinks, List<Allocation> result) {
        if (sources.isEmpty() || sinks.isEmpty()) return;

        // 计算总可用和总需求
        float totalAvailable = sources.stream().map(s -> s.remaining).reduce(0f, Float::sum);
        float totalDemand = sinks.stream().map(s -> s.remaining).reduce(0f, Float::sum);
        if (totalAvailable <= 0 || totalDemand <= 0) return;

        float transferAmount = Math.min(totalAvailable, totalDemand);

        // 按可用比例分配
        for (SourceInfo src : sources) {
            if (src.remaining <= 0) continue;
            float srcRatio = src.remaining / totalAvailable;
            float srcAlloc = transferAmount * srcRatio;

            for (SinkInfo sink : sinks) {
                if (sink.remaining <= 0) continue;
                float sinkRatio = sink.remaining / totalDemand;
                float alloc = srcAlloc * sinkRatio;
                if (alloc <= 0.001f) continue;

                // 确保不超过双方剩余
                alloc = Math.min(alloc, src.remaining);
                alloc = Math.min(alloc, sink.remaining);

                if (alloc > 0.001f) {
                    result.add(new Allocation(src.port, sink.port, alloc));
                    src.remaining -= alloc;
                    sink.remaining -= alloc;
                }
            }
        }
    }

    private void executeAllocations(World world, List<Allocation> allocations) {
        for (Allocation alloc : allocations) {
            // 从源机器扣除能量
            BlockEntity srcBe = world.getBlockEntity(alloc.sourcePort.machinePos);
            if (srcBe instanceof IElectricalMachine srcMachine) {
                srcMachine.receiveFlux(alloc.amount);
                srcBe.markDirty();
            }

            // 向目标机器注入能量
            BlockEntity dstBe = world.getBlockEntity(alloc.sinkPort.machinePos);
            if (dstBe instanceof IElectricalMachine dstMachine) {
                dstMachine.addFlux(alloc.amount);
                dstBe.markDirty();
            }
        }
    }


    private static class SourceInfo {
        final Port port;
        final IElectricalMachine machine;
        float remaining; // 当前还可提供的能量

        SourceInfo(Port port, IElectricalMachine machine, float remaining) {
            this.port = port;
            this.machine = machine;
            this.remaining = remaining;
        }
    }

    private static class SinkInfo {
        final Port port;
        final IElectricalMachine machine;
        float remaining; // 当前还可接受的能量

        SinkInfo(Port port, IElectricalMachine machine, float remaining) {
            this.port = port;
            this.machine = machine;
            this.remaining = remaining;
        }
    }
}
package com.nekotech.block.entity.api.electrical.conductor;

import com.nekotech.NekoTechnology;
import com.nekotech.block.entity.api.electrical.IElectricalMachine;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 智能能量分配器
 * 遵循规则：低速输入端口优先达到最大速度，所有输入端口的总抽取速度不超过所有输出端口的总输出速度
 */
public class EnergyDistributor {

    public static class Allocation {
        public final Port inputPort;
        public final Port outputPort;
        public final float amount;

        public Allocation(Port inputPort, Port outputPort, float amount) {
            this.inputPort = inputPort;
            this.outputPort = outputPort;
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
        // 1. 收集输入端口和输出端口
        List<InputPortInfo> inputPorts = collectInputPorts(world, group);
        List<OutputPortInfo> outputPorts = collectOutputPorts(world, group);

        if (inputPorts.isEmpty() || outputPorts.isEmpty()) {
            return new AllocationResult(Collections.emptyList(), 0);
        }

        // 2. 计算总容量
        float totalInputCapacity = calculateTotalInputCapacity(inputPorts);
        float totalOutputCapacity = calculateTotalOutputDemand(outputPorts);

        // 3. 可用能量是输入容量和输出容量的最小值
        float availableEnergy = Math.min(totalInputCapacity, totalOutputCapacity);

        if (availableEnergy <= 0) {
            return new AllocationResult(Collections.emptyList(), 0);
        }

        // 4. 智能分配
        List<Allocation> allocations = smartAllocate(inputPorts, outputPorts,
                availableEnergy, totalInputCapacity, totalOutputCapacity);

        // 5. 执行传输
        if (!allocations.isEmpty()) {
            executeAllocations(world, allocations);
        }

        float totalTransferred = allocations.stream().map(a -> a.amount).reduce(0f, Float::sum);

        // 调试日志
        if (totalTransferred > 0 && world.getTime() % 20 == 0) {
            NekoTechnology.LOGGER.debug("[能量分配器] 传输: {:.2f} NF, 输入口: {}, 输出口: {}",
                    totalTransferred, inputPorts.size(), outputPorts.size());
        }

        return new AllocationResult(allocations, totalTransferred);
    }

    private List<InputPortInfo> collectInputPorts(World world, ConductorGroup group) {
        List<InputPortInfo> inputs = new ArrayList<>();

        for (Port port : group.inputPorts) {
            BlockEntity be = world.getBlockEntity(port.machinePos);
            if (be instanceof IElectricalMachine machine) {
                float available = machine.getNekoFlux();
                float maxExtractable = Math.min(available, port.getEffectiveRate());

                if (maxExtractable > 0.001f) {
                    inputs.add(new InputPortInfo(port, machine, maxExtractable));
                }
            }
        }

        return inputs;
    }

    private List<OutputPortInfo> collectOutputPorts(World world, ConductorGroup group) {
        List<OutputPortInfo> outputs = new ArrayList<>();

        for (Port port : group.outputPorts) {
            BlockEntity be = world.getBlockEntity(port.machinePos);
            if (be instanceof IElectricalMachine machine) {
                float spaceAvailable = machine.getMaxNekoFlux() - machine.getNekoFlux();
                float maxAcceptable = Math.min(spaceAvailable, port.getEffectiveRate());

                if (maxAcceptable > 0) {
                    outputs.add(new OutputPortInfo(port, machine, maxAcceptable));
                }
            }
        }

        return outputs;
    }

    private float calculateTotalInputCapacity(List<InputPortInfo> inputs) {
        float total = 0;
        for (InputPortInfo info : inputs) {
            total += info.maxExtractable;
        }
        return total;
    }

    private float calculateTotalOutputDemand(List<OutputPortInfo> outputs) {
        float total = 0;
        for (OutputPortInfo info : outputs) {
            total += info.maxAcceptable;
        }
        return total;
    }

    private List<Allocation> smartAllocate(List<InputPortInfo> inputs, List<OutputPortInfo> outputs,
                                           float totalAllocated, float totalInputCapacity, float totalOutputCapacity) {
        List<Allocation> allocations = new ArrayList<>();
        if (outputs.isEmpty() || inputs.isEmpty() || totalAllocated <= 0) {
            return allocations;
        }

        float[] inputAllocations = calculateInputAllocations(inputs, totalAllocated, totalInputCapacity, totalOutputCapacity);

        float[] outputRatios = new float[outputs.size()];

        if (totalOutputCapacity > 0) {
            for (int j = 0; j < outputs.size(); j++) {
                outputRatios[j] = outputs.get(j).maxAcceptable / totalOutputCapacity;
            }
        } else {
            return allocations;
        }
        for (int i = 0; i < inputs.size(); i++) {
            InputPortInfo inputInfo = inputs.get(i);
            float inputAlloc = inputAllocations[i];

            if (inputAlloc <= 0) continue;

            for (int j = 0; j < outputs.size(); j++) {
                OutputPortInfo outputInfo = outputs.get(j);
                float allocationAmount = inputAlloc * outputRatios[j];

                // 避免极小的分配量
                if (allocationAmount > 0.001f) {
                    allocations.add(new Allocation(inputInfo.port, outputInfo.port, allocationAmount));
                }
            }
        }

        return allocations;
    }


    /**
     * 计算每个输入端口的分配量
     */
    private float[] calculateInputAllocations(List<InputPortInfo> inputs, float totalAllocated,
                                              float totalInputCapacity, float totalOutputCapacity) {
        float[] allocations = new float[inputs.size()];

        // 按最大速度升序排序（低速优先）
        List<InputPortInfo> sortedInputs = new ArrayList<>(inputs);
        sortedInputs.sort(Comparator.comparingDouble(a -> a.maxExtractable));

        if (totalOutputCapacity >= totalInputCapacity) {
            // 情况1：输出容量充足，所有输入端口可达到最大速度
            for (int i = 0; i < sortedInputs.size(); i++) {
                InputPortInfo input = sortedInputs.get(i);
                allocations[i] = input.maxExtractable;
            }
        } else {
            // 情况2：输出容量不足，需要公平分配
            float remainingCapacity = totalAllocated; // 剩余可分配容量
            int remainingInputs = sortedInputs.size();

            for (int i = 0; i < sortedInputs.size(); i++) {
                InputPortInfo input = sortedInputs.get(i);
                float average = remainingCapacity / remainingInputs;

                if (input.maxExtractable <= average) {
                    // 条件3：低速端口可满速运行
                    allocations[i] = input.maxExtractable;
                    remainingCapacity -= input.maxExtractable;
                } else {
                    // 按平均值分配
                    allocations[i] = average;
                    remainingCapacity -= average;
                }
                remainingInputs--;
            }
        }

        return allocations;
    }

    private void executeAllocations(World world, List<Allocation> allocations) {
        for (Allocation allocation : allocations) {
            // 从输入端扣除能量
            BlockEntity inputBe = world.getBlockEntity(allocation.inputPort.machinePos);
            if (inputBe instanceof IElectricalMachine inputMachine) {
                inputMachine.receiveFlux(allocation.amount);
                inputBe.markDirty();
            }

            // 向输出端添加能量
            BlockEntity outputBe = world.getBlockEntity(allocation.outputPort.machinePos);
            if (outputBe instanceof IElectricalMachine outputMachine) {
                outputMachine.addFlux(allocation.amount);
                outputBe.markDirty();
            }

            NekoTechnology.LOGGER.debug("[能量分配器] 传输: {:.2f} NF 从 {} 到 {}",
                    allocation.amount, allocation.inputPort.machinePos, allocation.outputPort.machinePos);
        }
    }

    private static class InputPortInfo {
        public final Port port;
        public final IElectricalMachine machine;
        public float maxExtractable;

        public InputPortInfo(Port port, IElectricalMachine machine, float maxExtractable) {
            this.port = port;
            this.machine = machine;
            this.maxExtractable = maxExtractable;
        }
    }

    private static class OutputPortInfo {
        public final Port port;
        public final IElectricalMachine machine;
        public float maxAcceptable;

        public OutputPortInfo(Port port, IElectricalMachine machine, float maxAcceptable) {
            this.port = port;
            this.machine = machine;
            this.maxAcceptable = maxAcceptable;
        }
    }
}

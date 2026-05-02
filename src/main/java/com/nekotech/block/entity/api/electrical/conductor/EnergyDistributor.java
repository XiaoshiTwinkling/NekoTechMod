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

        // 2. 计算总容量和需求
        float totalInputCapacity = calculateTotalInputCapacity(inputPorts);
        float totalOutputDemand = calculateTotalOutputDemand(outputPorts);

        // 3. 可用能量是输入容量和输出需求的最小值
        float availableEnergy = Math.min(totalInputCapacity, totalOutputDemand);

        // 4. 智能分配（低速输入端口优先）
        List<Allocation> allocations = smartAllocate(inputPorts, outputPorts, availableEnergy);

        // 5. 执行传输
        executeAllocations(world, allocations);

        return new AllocationResult(allocations, availableEnergy);
    }

    private List<InputPortInfo> collectInputPorts(World world, ConductorGroup group) {
        List<InputPortInfo> inputs = new ArrayList<>();

        for (Port port : group.inputPorts) {
            BlockEntity be = world.getBlockEntity(port.machinePos);
            if (be instanceof IElectricalMachine machine) {
                float availableEnergy = machine.getNekoFlux();
                float maxExtractable = Math.min(availableEnergy, port.getEffectiveRate());

                if (maxExtractable > 0) {
                    inputs.add(new InputPortInfo(port, machine, maxExtractable));
                }
            }
        }

        // 按最大可提取量升序排序（低速优先）
        inputs.sort(Comparator.comparingDouble(a -> a.maxExtractable));

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

    private List<Allocation> smartAllocate(List<InputPortInfo> inputs, List<OutputPortInfo> outputs, float totalAvailable) {
        List<Allocation> allocations = new ArrayList<>();

        if (outputs.isEmpty()) {
            return allocations;
        }

        // 计算总输出容量
        float totalOutputCapacity = 0;
        for (OutputPortInfo output : outputs) {
            totalOutputCapacity += output.maxAcceptable;
        }

        // 总输入不能超过总输出
        float effectiveAvailable = Math.min(totalAvailable, totalOutputCapacity);

        // 优先满足低速输入端口
        float remaining = effectiveAvailable;

        for (InputPortInfo input : inputs) {
            if (remaining <= 0) break;

            // 为此输入端口分配能量（不能超过其最大速率）
            // 每个input端口的实际抽取速率 ≤ 该端口的最大速率
            float maxForThisInput = input.maxExtractable;
            float toAllocate = Math.min(maxForThisInput, remaining);

            if (toAllocate <= 0) continue;

            List<Allocation> inputAllocations = allocateForInput(input, outputs, toAllocate);

            allocations.addAll(inputAllocations);
            remaining -= toAllocate;

            // 更新输出端口剩余容量
            for (Allocation alloc : inputAllocations) {
                for (OutputPortInfo output : outputs) {
                    if (output.port == alloc.outputPort) {
                        output.maxAcceptable -= alloc.amount;
                        break;
                    }
                }
            }
        }

        return allocations;
    }

    private List<Allocation> allocateForInput(InputPortInfo input, List<OutputPortInfo> outputs, float available) {
        List<Allocation> allocations = new ArrayList<>();

        // 计算输出端口总剩余需求
        float totalRemainingDemand = 0;
        for (OutputPortInfo output : outputs) {
            if (output.maxAcceptable > 0) {
                totalRemainingDemand += output.maxAcceptable;
            }
        }

        if (totalRemainingDemand <= 0) {
            return allocations;
        }

        // 按需求比例分配
        for (OutputPortInfo output : outputs) {
            if (output.maxAcceptable <= 0) continue;

            float allocationRatio = output.maxAcceptable / totalRemainingDemand;
            float allocated = available * allocationRatio;

            // 确保不超过输出端口的最大接受量
            allocated = Math.min(allocated, output.maxAcceptable);

            if (allocated > 0) {
                allocations.add(new Allocation(input.port, output.port, allocated));
                available -= allocated;
                output.maxAcceptable -= allocated;

                if (available <= 0) break;
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

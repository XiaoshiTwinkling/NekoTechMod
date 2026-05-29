package com.nekotech.block.entity.api;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 这个接口表示 这个方块是一个可以控制机器的方块 比如猫猫坐垫w
 * 可以管理多个机器喵
 */
public interface ICatControlBlock {

    /**
     * 获取这个控制器的唯一ID喵~
     * 用于区分不同的控制器，通常是位置或其他唯一标识喵~
     */
    String getControllerId();

    /**
     * 检查这个控制器是否处于活跃状态（可控制）喵~
     * 默认总是返回 true，子类可覆盖此逻辑喵~
     */
    default boolean isControllerActive() {
        return true;
    }

    /**
     * 获取这个控制器可以控制的最大机器数量喵~
     * 返回 -1 表示无限制喵~
     */
    default int getMaxControlledMachines() {
        return 4;  // 默认最多控制4个机器
    }

    /**
     * 检查这个控制器是否可以控制指定的机器喵~
     * 默认实现总是返回 true，子类可添加特定逻辑喵~
     */
    default boolean canControlMachine(World world, BlockPos machinePos, BlockState machineState) {
        return true;
    }

    /**
     * 当控制器状态发生变化时调用喵~
     * 比如从激活变为非激活，或反之喵~
     */
    default void onControllerStateChanged(World world, BlockPos pos, BlockState state, boolean newState) {
        // 子类可覆盖此方法，实现状态变化时的逻辑
    }

    /**
     * 获取这个控制器当前控制的所有机器位置喵~
     */
    @Nullable
    List<BlockPos> getControlledMachines();

    /**
     * 注册一个机器到这个控制器喵~
     * 返回注册是否成功喵~
     */
    boolean registerMachine(BlockPos machinePos);

    /**
     * 从控制器中注销一个机器喵~
     * 返回注销是否成功喵~
     */
    boolean unregisterMachine(BlockPos machinePos);

    /**
     * 检查指定的机器是否已注册到这个控制器喵~
     */
    boolean isMachineRegistered(BlockPos machinePos);

    /**
     * 清理无效的机器注册喵~
     * 比如机器方块已被移除的情况喵~
     */
    void cleanupInvalidMachines();

    /**
     * 获取控制器的控制范围（以方块为单位）喵~
     * 返回控制器可以控制机器的最大距离喵~
     */
    default int getControlRange() {
        return 3;  // 默认3格范围内
    }

    /**
     * 检查指定位置是否在控制范围内喵~
     */
    default boolean isInControlRange(BlockPos controllerPos, BlockPos machinePos) {
        int range = getControlRange();
        return controllerPos.getManhattanDistance(machinePos) <= range;
    }

    /**
     * 获取控制器类型喵~
     * 用于区分不同类型的控制器（比如猫猫坐垫、控制台等）喵~
     */
    String getControllerType();
}

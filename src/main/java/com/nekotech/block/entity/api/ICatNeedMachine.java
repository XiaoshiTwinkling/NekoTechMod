package com.nekotech.block.entity.api;

import com.nekotech.block.entity.CushionBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public interface ICatNeedMachine {

    /**
     * 获取这个机器所属的世界喵~
     * 默认实现从 BlockEntity 获取喵~
     */
    default World getMachineWorld() {
        return ((BlockEntity) this).getWorld();
    }

    /**
     * 获取这个机器的位置喵~
     * 默认实现从 BlockEntity 获取喵~
     */
    default BlockPos getMachinePos() {
        return ((BlockEntity) this).getPos();
    }

    /**
     * 检查这个机器是否可以运行喵~
     * 默认实现检查是否有绑定的控制器，并且控制器处于活跃状态喵~
     */
    default boolean canMachineRun() {
        World world = getMachineWorld();
        if (world == null || world.isClient) return false;

        // 获取绑定的控制器
        ICatControlBlock controller = getBoundController();

        // 如果没有绑定的控制器，尝试自动绑定
        if (controller == null) {
            controller = findAndBindController();
        }

        // 检查控制器是否存在且活跃
        return controller != null && controller.isControllerActive();
    }

    /**
     * 自动查找并绑定附近的控制器喵~
     * 返回绑定的控制器，如果找不到则返回 null 喵~
     */
    @Nullable
    default ICatControlBlock findAndBindController() {
        World world = getMachineWorld();
        if (world == null || world.isClient) return null;

        BlockPos machinePos = getMachinePos();
        BlockPos controllerPos = findNearestController(world, machinePos);

        if (controllerPos != null) {
            BlockEntity be = world.getBlockEntity(controllerPos);
            if (be instanceof ICatControlBlock controller) {
                // 尝试注册到控制器
                if (controller.registerMachine(machinePos)) {
                    setBoundControllerPos(controllerPos);
                    return controller;
                }
            }
        }

        return null;
    }

    /**
     * 查找最近的控制器喵~
     * 返回控制器的位置，如果找不到则返回 null 喵~
     */
    @Nullable
    default BlockPos findNearestController(World world, BlockPos machinePos) {
        int searchRange = 8;  // 搜索范围

        for (int dx = -searchRange; dx <= searchRange; dx++) {
            for (int dy = -searchRange; dy <= searchRange; dy++) {
                for (int dz = -searchRange; dz <= searchRange; dz++) {
                    BlockPos checkPos = machinePos.add(dx, dy, dz);
                    BlockEntity be = world.getBlockEntity(checkPos);

                    if (be instanceof ICatControlBlock controller) {
                        // 检查控制器是否活跃且可以控制此机器
                        if (controller.isControllerActive() &&
                                controller.canControlMachine(world, machinePos, world.getBlockState(machinePos)) &&
                                controller.isInControlRange(checkPos, machinePos)) {
                            return checkPos;
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * 获取绑定的控制器位置喵~
     * 由实现类从 NBT 中读取并返回喵~
     */
    @Nullable
    BlockPos getBoundControllerPos();

    /**
     * 设置绑定的控制器位置喵~
     * 由实现类保存到 NBT 中喵~
     */
    void setBoundControllerPos(@Nullable BlockPos pos);

    /**
     * 获取绑定的控制器喵~
     * 返回控制器实例，如果位置无效或控制器不存在则返回 null 喵~
     */
    @Nullable
    default ICatControlBlock getBoundController() {
        BlockPos controllerPos = getBoundControllerPos();
        if (controllerPos == null) return null;

        World world = getMachineWorld();
        if (world == null || world.isClient) return null;

        BlockEntity be = world.getBlockEntity(controllerPos);
        if (be instanceof ICatControlBlock controller) {
            // 验证控制器是否确实控制了此机器
            if (controller.isMachineRegistered(getMachinePos())) {
                return controller;
            }
        }

        // 如果控制器不存在或未注册此机器，清除绑定
        setBoundControllerPos(null);
        return null;
    }

    /**
     * 解除与控制器的绑定喵~
     * 从控制器中注销此机器喵~
     */
    default void unbindController() {
        ICatControlBlock controller = getBoundController();
        if (controller != null) {
            controller.unregisterMachine(getMachinePos());
        }
        setBoundControllerPos(null);
    }

    /**
     * 检查是否有活跃的控制器绑定喵~
     */
    default boolean hasActiveController() {
        ICatControlBlock controller = getBoundController();
        return controller != null && controller.isControllerActive();
    }

    /**
     * 检查控制器是否在线（存在于世界中）喵~
     */
    default boolean isControllerOnline() {
        BlockPos controllerPos = getBoundControllerPos();
        if (controllerPos == null) return false;

        World world = getMachineWorld();
        if (world == null) return false;

        BlockEntity be = world.getBlockEntity(controllerPos);
        return be instanceof ICatControlBlock;
    }

    /**
     * 清理无效的绑定喵~
     * 当控制器被破坏或离线时调用喵~
     */
    default void cleanupInvalidBindings() {
        if (!isControllerOnline()) {
            setBoundControllerPos(null);
        }
    }

    /**
     * 当机器方块被破坏时调用，通知控制器注销此机器喵~
     * 默认实现会查找并通知所有绑定的控制器喵~
     */
    default void onMachineRemoved() {
        ICatControlBlock controller = getBoundController();
        if (controller != null) {
            controller.unregisterMachine(getMachinePos());
        }
    }
}
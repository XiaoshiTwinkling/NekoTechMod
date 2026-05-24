package com.nekotech.block.entity.api.electrical.conductor;

import com.nekotech.NekoTechnology;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public class ConductorSystem implements ModInitializer {
    @Override
    public void onInitialize() {
        NekoTechnology.LOGGER.info("导体系统初始化");

        // 注册世界tick事件
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (!world.isClient()) {
                ConductorManager manager = ConductorManager.get(world);
                if (manager != null) {
                    manager.tick(world);
                }
            }
        });

        // 注册世界加载/卸载事件
        ServerWorldEvents.LOAD.register((server, world) -> {
            NekoTechnology.LOGGER.info("[导体系统] 世界加载: {}", world.getRegistryKey().getValue());
        });

        ServerWorldEvents.UNLOAD.register((server, world) -> {
            NekoTechnology.LOGGER.info("[导体系统] 世界卸载: {}", world.getRegistryKey().getValue());
        });

        NekoTechnology.LOGGER.info("[导体系统] 初始化完成");
    }

    public static void onBlockPlaced(ServerWorld world, net.minecraft.util.math.BlockPos pos) {
        ConductorManager manager = ConductorManager.get(world);
        if (manager != null) {
            manager.onBlockPlaced(world, pos);
        }
    }

    public static void onBlockBroken(ServerWorld world, net.minecraft.util.math.BlockPos pos) {
        ConductorManager manager = ConductorManager.get(world);
        if (manager != null) {
            manager.onBlockBroken(world, pos);
        }
    }

    public static void onComponentChanged(ServerWorld world, net.minecraft.util.math.BlockPos pos,
                                          net.minecraft.util.math.Direction side) {
        ConductorManager manager = ConductorManager.get(world);
        if (manager != null) {
            manager.onComponentChanged(world, pos, side);
        }
    }

    /**
     * 当导体方块实体状态变化时调用喵~
     * 用于通知导体系统重新扫描网络
     *
     * @param world 服务器世界
     * @param pos 方块位置
     */
    public static void onBlockEntityStateChange(ServerWorld world, BlockPos pos) {
        ConductorManager manager = ConductorManager.get(world);
        if (manager != null) {
            manager.onBlockEntityStateChange(world, pos);
        }
    }
}

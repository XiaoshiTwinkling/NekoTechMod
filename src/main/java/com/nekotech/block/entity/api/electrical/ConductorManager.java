package com.nekotech.block.entity.api.electrical;

import com.nekotech.NekoTechnology;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * 管理一个世界中的所有导体网络喵~
 * 那我是猫猫王了
 */
public class ConductorManager {
    // 单例访问
    private static final String DATA_KEY = "neko_technology_conductor_manager";

    // 方块位置 -> 所属网络 的映射喵
    private final Map<BlockPos, Conductor> blockToNetwork = new Object2ObjectOpenHashMap<>();
    // 所有活跃的网络喵
    private final Set<Conductor> networks = new HashSet<>();

    private static final Map<World, ConductorManager> INSTANCES = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 获取或创建指定世界的管理器
     */
    public static ConductorManager get(World world) {
        if (world.isClient) {
            return null;
        }
        return INSTANCES.computeIfAbsent(world, w -> new ConductorManager());
    }

    /**
     * 当某个位置的导体状态发生变化时调用（如方块放置、破坏、零件变更）。
     * 这将触发以该位置为中心的网络重建。
     */
    /**
     * 当某个位置的导体状态发生变化时调用
     */
    public void invalidateAt(BlockPos pos) {
        NekoTechnology.LOGGER.info("[导体管理器] 方块 {} 状态变化，触发网络重建", pos);

        // 查找并移除所有包含此方块的旧网络
        Set<Conductor> affectedNetworks = new HashSet<>();
        Conductor existingNet = blockToNetwork.get(pos);
        if (existingNet != null) {
            affectedNetworks.add(existingNet);
        }

        // 也需要检查相邻方块
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.offset(dir);
            Conductor neighborNet = blockToNetwork.get(neighbor);
            if (neighborNet != null) {
                affectedNetworks.add(neighborNet);
            }
        }

        // 销毁受影响的旧网络
        for (Conductor net : affectedNetworks) {
            removeNetwork(net);
        }

        // 将位置加入待重建队列
        pendingRebuildPositions.add(pos);
        for (Conductor net : affectedNetworks) {
            pendingRebuildPositions.addAll(net.getBlocks());
        }
    }

    // 待重建的方块队列
    private final Set<BlockPos> pendingRebuildPositions = new HashSet<>();

    /**
     * 计划在下一个 tick 重建指定位置的网络
     */
    public void scheduleRebuildAt(BlockPos pos) {
        pendingRebuildPositions.add(pos);
    }

    /**
     * 处理待重建的网络
     */
    public void tick(World world) {
        // 处理待重建的网络

        if (!pendingRebuildPositions.isEmpty()) {
            // 复制一份以防止在迭代中修改
            Set<BlockPos> toRebuild = new HashSet<>(pendingRebuildPositions);
            pendingRebuildPositions.clear();

            for (BlockPos pos : toRebuild) {
                // 只有当该位置确实是导体时，才以其为起点重建
                if (world.getBlockEntity(pos) instanceof ITransferElectrical) {
                    rebuildNetworkFrom(world, pos);
                }
            }
        }

        // 驱动所有网络 tick
        for (Conductor net : networks) {
            net.tick(world);
        }
    }

    /**
     * 从指定位置开始，重建一个导体网络
     */
    private void rebuildNetworkFrom(World world, BlockPos startPos) {
        // 如果这个位置已经属于某个网络，且该网络包含此位置，则跳过
        // (可能在批量重建时重复)
        if (blockToNetwork.containsKey(startPos)) {
            return;
        }

        Conductor newNetwork = new Conductor();
        newNetwork.rebuild(world, startPos);

        // 如果新网络没有方块（不应该发生） 丢弃
        if (newNetwork.getBlocks().isEmpty()) {
            return;
        }

        // 检查新网络的方块是否已属于其他网络（理论上不应该，因为之前已移除）
        for (BlockPos pos : newNetwork.getBlocks()) {
            Conductor existing = blockToNetwork.get(pos);
            if (existing != null && existing != newNetwork) {
                // 强制移除旧映射
                blockToNetwork.remove(pos);
            }
        }

        // 注册新网络
        addNetwork(newNetwork);
    }

    /**
     * 注册一个新网络
     */
    private void addNetwork(Conductor network) {
        networks.add(network);
        for (BlockPos pos : network.getBlocks()) {
            blockToNetwork.put(pos, network);
        }
    }

    /**
     * 移除一个网络
     */
    private void removeNetwork(Conductor network) {
        if (networks.remove(network)) {
            for (BlockPos pos : network.getBlocks()) {
                blockToNetwork.remove(pos);
            }
        }
    }

    /**
     * 获取包含指定方块的网络
     */
    @Nullable
    public Conductor getNetworkAt(BlockPos pos) {
        return blockToNetwork.get(pos);
    }

    /**
     * 获取所有网络
     */
    public Collection<Conductor> getAllNetworks() {
        return Collections.unmodifiableCollection(networks);
    }

    @Override
    public String toString() {
        return String.format("ConductorManager{networks=%d, blocks=%d}", networks.size(), blockToNetwork.size());
    }
}

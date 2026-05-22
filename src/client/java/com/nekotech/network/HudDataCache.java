package com.nekotech.network;

import com.nekotech.item.api.googles.GoogleAbstractHUD;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class HudDataCache {
    private static final HashMap<BlockPos, GoogleAbstractHUD> currentData = new HashMap<>();

    private static final java.util.HashMap<BlockPos, java.util.List<GoogleAbstractHUD>> hudListCache = new java.util.HashMap<>();

    private static final HashMap<BlockPos, Long> lastUpdateTime = new HashMap<>();
    private static final long CACHE_TIMEOUT_MS = 1000; // 1秒超时
    /**
     * 存储HUD数据
     */
    public static void storeHudData(BlockPos pos, GoogleAbstractHUD hudData) {
        currentData.put(pos, hudData);
    }

    /**
     * 获取HUD数据
     */
    @Nullable
    public static GoogleAbstractHUD getHudData(BlockPos pos) {
        return currentData.get(pos);
    }

    /**
     * 移除HUD数据
     */
    public static void removeHudData(BlockPos pos) {
        currentData.remove(pos);
        hudListCache.remove(pos);
        lastUpdateTime.remove(pos);
    }

    /**
     * 检查数据是否存在
     */
    public static boolean hasData(BlockPos pos) {
        return currentData.containsKey(pos);
    }

    /**
     * 清除所有数据
     */
    public static void clearAll() {
        currentData.clear();
    }

    /**
     * 存储HUD列表到缓存喵~
     */
    public static void storeHudDataList(BlockPos pos, java.util.List<GoogleAbstractHUD> huds) {
        hudListCache.put(pos, huds);
        lastUpdateTime.put(pos, System.currentTimeMillis());
    }

    /**
     * 从缓存获取HUD列表喵~
     */
    @Nullable
    public static java.util.List<GoogleAbstractHUD> getHudDataList(BlockPos pos) {
        Long lastTime = lastUpdateTime.get(pos);
        if (lastTime != null && System.currentTimeMillis() - lastTime > CACHE_TIMEOUT_MS) {
            // 缓存超时，自动清理
            removeHudData(pos);
            return null;
        }
        return hudListCache.get(pos);
    }




}

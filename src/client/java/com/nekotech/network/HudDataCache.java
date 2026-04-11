package com.nekotech.network;

import com.nekotech.item.api.googles.GoogleAbstractHUD;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class HudDataCache {
    private static final HashMap<BlockPos, GoogleAbstractHUD> currentData = new HashMap<>();

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
}

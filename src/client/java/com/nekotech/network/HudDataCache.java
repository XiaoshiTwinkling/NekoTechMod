package com.nekotech.network;

import com.nekotech.item.api.googles.GoogleAbstractHUD;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class HudDataCache {
    private static final Map<BlockPos, CacheEntry> cache = new HashMap<>();
    private static long lastCleanup = 0;

    private static class CacheEntry {
        final GoogleAbstractHUD hudData;
        final long timestamp;

        CacheEntry(GoogleAbstractHUD hudData) {
            this.hudData = hudData;
            this.timestamp = System.currentTimeMillis();
        }
    }

    /**
     * 存储HUD数据
     */
    public static void storeHudData(BlockPos pos, GoogleAbstractHUD hudData) {
        cache.put(pos, new CacheEntry(hudData));
        cleanupExpiredEntries();
    }

    /**
     * 获取HUD数据
     */
    @Nullable
    public static GoogleAbstractHUD getHudData(BlockPos pos) {
        CacheEntry entry = cache.get(pos);
        if (entry == null) {
            return null;
        }

        // 检查是否过期（5秒）
        if (isExpired(entry.timestamp, 5000)) {
            cache.remove(pos);
            return null;
        }

        return entry.hudData;
    }

    /**
     * 检查数据是否有效
     */
    public static boolean isDataValid(BlockPos pos) {
        CacheEntry entry = cache.get(pos);
        if (entry == null) {
            return false;
        }
        return !isExpired(entry.timestamp, 5000);
    }

    /**
     * 移除HUD数据
     */
    public static void removeHudData(BlockPos pos) {
        cache.remove(pos);
    }

    /**
     * 检查时间戳是否过期
     */
    private static boolean isExpired(long timestamp, long maxAge) {
        return System.currentTimeMillis() - timestamp > maxAge;
    }

    /**
     * 清理过期缓存
     */
    private static void cleanupExpiredEntries() {
        long now = System.currentTimeMillis();
        if (now - lastCleanup < 10000) { // 每10秒清理一次
            return;
        }

        lastCleanup = now;
        cache.entrySet().removeIf(entry ->
                isExpired(entry.getValue().timestamp, 5000)
        );
    }
}

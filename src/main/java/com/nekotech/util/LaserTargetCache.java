package com.nekotech.util;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LaserTargetCache {

    private static final Map<UUID, Vec3d> TARGETS = new HashMap<>();

    public static void set(PlayerEntity player, Vec3d pos) {
        TARGETS.put(player.getUuid(), pos);
    }

    public static void remove(PlayerEntity player) {
        TARGETS.remove(player.getUuid());
    }

    public static boolean isEmpty() {
        return TARGETS.isEmpty();
    }

    public static Vec3d getNearest(Vec3d origin, double maxDistance) {
        Vec3d nearest = null;
        double best = maxDistance * maxDistance;

        for (Vec3d pos : TARGETS.values()) {
            double dist = pos.squaredDistanceTo(origin);
            if (dist < best) {
                best = dist;
                nearest = pos;
            }
        }

        return nearest;
    }

    public static boolean contains(Vec3d pos) {
        return TARGETS.containsValue(pos);
    }
}
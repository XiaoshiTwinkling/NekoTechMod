package com.nekotech.catcamera;

import java.util.UUID;

public final class CatCameraClientState {
    private static boolean active;
    private static UUID catUuid;

    private CatCameraClientState() {}
    public static boolean isActive() { return active; }
    public static UUID getCatUuid() { return catUuid; }
    public static void set(boolean value, UUID target) { active = value; catUuid = value ? target : null; }
}

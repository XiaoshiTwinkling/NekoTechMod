package com.nekotech.events;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;

public class ServerTick {
    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(ServerTick::tick);
    }

    private static void tick(MinecraftServer server) {
    }
}

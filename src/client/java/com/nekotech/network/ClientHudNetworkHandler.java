package com.nekotech.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;

public class ClientHudNetworkHandler {
    public static void initialize() {
        // 注册接收HUD数据的处理器
        ClientPlayNetworking.registerGlobalReceiver(
                HudNetworkPayloads.SEND_HUD_DATA,
                (payload, context) -> handleHudData(payload, context)
        );
    }

    private static void handleHudData(
            HudNetworkPayloads.SendHudDataPayload payload,
            ClientPlayNetworking.Context context
    ) {
        BlockPos pos = payload.pos();
        NbtCompound nbt = payload.data();

        // 在主线程处理
        context.client().execute(() -> {
            var client = net.minecraft.client.MinecraftClient.getInstance();

            if (client.world != null) {
                var registries = client.world.getRegistryManager();
                var hudData = com.nekotech.network.HudNetworkHandler.deserializeHudData(nbt, registries);

                if (hudData != null) {
                    HudDataCache.storeHudData(pos, hudData);
                }
            } else {
                com.nekotech.NekoTechnology.LOGGER.warn("客户端世界为 null，无法获取注册表");
            }
        });
    }

    /**
     * 向服务端请求HUD数据
     */
    public static void requestHudData(BlockPos pos) {
        ClientPlayNetworking.send(
                new HudNetworkPayloads.RequestHudDataPayload(pos)
        );
    }
}

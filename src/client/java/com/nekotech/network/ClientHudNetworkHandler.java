package com.nekotech.network;

import com.nekotech.item.api.googles.GoogleAbstractHUD;
import com.nekotech.renderer.ClientLaserTargetCache;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

public class ClientHudNetworkHandler {
    public static void initialize() {
        // 注册接收HUD数据的处理器
        ClientPlayNetworking.registerGlobalReceiver(
                HudNetworkPayloads.SEND_HUD_DATA,
                ClientHudNetworkHandler::handleHudData
        );
        ClientPlayNetworking.registerGlobalReceiver(
                HudNetworkPayloads.SEND_RAY_POS,
                ClientHudNetworkHandler::handleRayPos
        );
        ClientPlayNetworking.registerGlobalReceiver(
                HudNetworkPayloads.REMOVE_RAY_POS,
                ClientHudNetworkHandler::handleRemoveRayPos
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
                java.util.List<GoogleAbstractHUD> hudList = com.nekotech.network.HudNetworkHandler.deserializeHudList(nbt, registries, pos);

                if (hudList != null && !hudList.isEmpty()) {
                    HudDataCache.storeHudDataList(pos, hudList);
                }
            }
        });
    }

    private static void handleRayPos(
            HudNetworkPayloads.SendRayPosPayload payload,
            ClientPlayNetworking.Context context
    ) {
        UUID uuid = payload.uuid();
        double x = payload.x();
        double y = payload.y();
        double z = payload.z();

        context.client().execute(() -> {
            ClientLaserTargetCache.set(uuid, new Vec3d(x, y, z));
        });
    }

    private static void handleRemoveRayPos(
            HudNetworkPayloads.RemoveRayPosPayload payload,
            ClientPlayNetworking.Context context
    ) {
        UUID uuid = payload.uuid();

        context.client().execute(() -> {
            ClientLaserTargetCache.remove(uuid);
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

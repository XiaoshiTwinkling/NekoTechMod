package com.nekotech.network;

import com.nekotech.item.api.googles.GoogleAbstractHUD;
import com.nekotech.network.payload.c2s.RequestHudDataPayload;
import com.nekotech.network.payload.s2c.*;
import com.nekotech.renderer.ClientLaserTargetCache;
import com.nekotech.renderer.components.WirePoleRenderer;
import com.nekotech.screens.NekoTagListScreen;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.UUID;

public class ClientHudNetworkHandler {
    public static void initialize() {
        ClientPlayNetworking.registerGlobalReceiver(
                SendHudDataPayload.ID,
                ClientHudNetworkHandler::handleHudData
        );

        ClientPlayNetworking.registerGlobalReceiver(
                SendRayPosPayload.ID,
                ClientHudNetworkHandler::handleRayPos
        );

        ClientPlayNetworking.registerGlobalReceiver(
                RemoveRayPosPayload.ID,
                ClientHudNetworkHandler::handleRemoveRayPos
        );

        ClientPlayNetworking.registerGlobalReceiver(
                OpenTagListPayload.ID,
                ClientHudNetworkHandler::handleOpenTagList
        );

        ClientPlayNetworking.registerGlobalReceiver(SyncWirePairsPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                WirePoleRenderer.updatePairs(payload.pairs());
            });
        });
    }

    private static void handleOpenTagList(
            OpenTagListPayload payload,
            ClientPlayNetworking.Context context
    ) {
        context.client().execute(() -> {
            context.client().setScreen(
                    new NekoTagListScreen(payload.pos(), payload.tags())
            );
        });
    }

    private static void handleHudData(
            SendHudDataPayload payload,
            ClientPlayNetworking.Context context
    ) {
        BlockPos pos = payload.pos();
        NbtCompound nbt = payload.data();

        context.client().execute(() -> {
            MinecraftClient client = MinecraftClient.getInstance();

            if (client.world == null) {
                return;
            }

            var registries = client.world.getRegistryManager();

            List<GoogleAbstractHUD> hudList =
                    NetworkHandler.deserializeHudList(nbt, registries, pos);

            if (hudList != null && !hudList.isEmpty()) {
                HudDataCache.storeHudDataList(pos, hudList);
            }
        });
    }

    private static void handleRayPos(
            SendRayPosPayload payload,
            ClientPlayNetworking.Context context
    ) {
        UUID uuid = payload.uuid();
        Vec3d pos = new Vec3d(payload.x(), payload.y(), payload.z());

        context.client().execute(() -> {
            ClientLaserTargetCache.set(uuid, pos);
        });
    }

    private static void handleRemoveRayPos(
            RemoveRayPosPayload payload,
            ClientPlayNetworking.Context context
    ) {
        UUID uuid = payload.uuid();

        context.client().execute(() -> {
            ClientLaserTargetCache.remove(uuid);
        });
    }

    /**
     * 向服务端请求 HUD 数据
     */
    public static void requestHudData(BlockPos pos) {
        ClientPlayNetworking.send(
                new RequestHudDataPayload(pos)
        );
    }
}
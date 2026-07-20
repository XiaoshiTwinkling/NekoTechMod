package com.nekotech.catcamera;

import com.nekotech.data.worlddata.CatCameraChannelWorldState;
import com.nekotech.data.worlddata.CatCameraViewSessionWorldState;
import com.nekotech.entity.CatCameraBodyEntity;
import com.nekotech.entity.ModEntities;
import com.nekotech.network.payload.s2c.CatCameraActionResultPayload;
import com.nekotech.network.payload.s2c.CatCameraViewStatePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.Map;
import java.util.UUID;

public final class CatCameraViewManager {
    private CatCameraViewManager() {}

    public static boolean isViewing(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        return server != null && CatCameraViewSessionWorldState.get(server).get(player.getUuid()) != null;
    }

    public static boolean enter(ServerPlayerEntity player, UUID catUuid) {
        MinecraftServer server = player.getServer();
        if (server == null) return false;
        CatCameraChannelData channel = CatCameraChannelWorldState.get(server).get(catUuid);
        if (channel == null || !channel.active()) {
            fail(player, "message.neko-technology.cat_camera.channel_missing");
            return false;
        }

        Identifier dimensionId = Identifier.tryParse(channel.dimension());
        if (dimensionId == null) {
            fail(player, "message.neko-technology.cat_camera.target_unavailable");
            return false;
        }
        ServerWorld targetWorld = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, dimensionId));
        if (targetWorld == null) {
            fail(player, "message.neko-technology.cat_camera.target_unavailable");
            return false;
        }

        targetWorld.getChunk(channel.chunkX(), channel.chunkZ());
        Entity entity = targetWorld.getEntity(catUuid);
        if (!(entity instanceof CatEntity cat) || !cat.isAlive()
                || !(cat instanceof CatCameraChannelAccess access)
                || !access.neko_technology$isCatCameraChannelActive()) {
            fail(player, "message.neko-technology.cat_camera.target_unavailable");
            return false;
        }

        exit(player, false);
        CatCameraViewSessionWorldState state = CatCameraViewSessionWorldState.get(server);
        CatCameraBodyEntity proxy = new CatCameraBodyEntity(ModEntities.CAT_CAMERA_BODY, player.getServerWorld());
        proxy.refreshPositionAndAngles(player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch());
        proxy.copyAppearance(player);
        player.getServerWorld().spawnEntity(proxy);

        state.put(player.getUuid(), new CatCameraViewSessionWorldState.Session(
                catUuid,
                player.getServerWorld().getRegistryKey().getValue().toString(),
                player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch(),
                player.interactionManager.getGameMode(),
                proxy.getUuid()
        ));

        player.changeGameMode(net.minecraft.world.GameMode.SPECTATOR);
        player.setCameraEntity(cat);
        ServerPlayNetworking.send(player, new CatCameraViewStatePayload(true, catUuid));
        return true;
    }

    public static void exit(ServerPlayerEntity player, boolean notifyClient) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        CatCameraViewSessionWorldState state = CatCameraViewSessionWorldState.get(server);
        CatCameraViewSessionWorldState.Session session = state.get(player.getUuid());
        if (session == null) {
            if (notifyClient) ServerPlayNetworking.send(player, new CatCameraViewStatePayload(false, null));
            return;
        }

        player.setCameraEntity(player);
        Identifier originId = Identifier.tryParse(session.originDimension());
        ServerWorld origin = originId == null ? null : server.getWorld(RegistryKey.of(RegistryKeys.WORLD, originId));
        if (origin == null) origin = server.getOverworld();
        Entity proxy = origin.getEntity(session.proxyUuid());
        if (proxy instanceof CatCameraBodyEntity) proxy.discard();
        player.teleport(origin, session.x(), session.y(), session.z(), session.yaw(), session.pitch());
        player.changeGameMode(session.gameMode());
        state.remove(player.getUuid());
        if (notifyClient) ServerPlayNetworking.send(player, new CatCameraViewStatePayload(false, null));
    }

    public static void exitWatchers(MinecraftServer server, UUID catUuid) {
        for (Map.Entry<UUID, CatCameraViewSessionWorldState.Session> entry
                : CatCameraViewSessionWorldState.get(server).entries()) {
            if (entry.getValue().targetCatUuid().equals(catUuid)) {
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
                if (player != null) exit(player, true);
            }
        }
    }

    public static void tick(MinecraftServer server) {
        for (Map.Entry<UUID, CatCameraViewSessionWorldState.Session> entry
                : CatCameraViewSessionWorldState.get(server).entries()) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
            if (player == null) continue;
            Entity camera = player.getCameraEntity();
            CatCameraChannelData channel = CatCameraChannelWorldState.get(server).get(entry.getValue().targetCatUuid());
            if (!(camera instanceof CatEntity) || !camera.isAlive()
                    || channel == null || !channel.active()) {
                exit(player, true);
            }
        }
    }

    private static void fail(ServerPlayerEntity player, String key) {
        ServerPlayNetworking.send(player, new CatCameraActionResultPayload(false, false, key));
    }
}

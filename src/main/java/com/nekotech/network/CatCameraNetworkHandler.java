package com.nekotech.network;

import com.nekotech.data.worlddata.CatCameraChannelWorldState;
import com.nekotech.catcamera.CatCameraChannelAccess;
import com.nekotech.catcamera.CatCameraChannelData;
import com.nekotech.catcamera.CatCameraChannelService;
import com.nekotech.catcamera.CatCameraViewManager;
import com.nekotech.item.ModItems;
import com.nekotech.network.payload.c2s.CreateCatCameraChannelPayload;
import com.nekotech.network.payload.c2s.EnterCatCameraChannelPayload;
import com.nekotech.network.payload.c2s.ExitCatCameraViewPayload;
import com.nekotech.network.payload.s2c.CatCameraActionResultPayload;
import com.nekotech.network.payload.s2c.CatCameraViewStatePayload;
import com.nekotech.network.payload.s2c.OpenCatCameraListPayload;
import com.nekotech.network.payload.s2c.OpenCatCameraNamePayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.server.network.ServerPlayerEntity;

public final class CatCameraNetworkHandler {
    private CatCameraNetworkHandler() {}

    public static void initialize() {
        PayloadTypeRegistry.playC2S().register(CreateCatCameraChannelPayload.ID, CreateCatCameraChannelPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(EnterCatCameraChannelPayload.ID, EnterCatCameraChannelPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ExitCatCameraViewPayload.ID, ExitCatCameraViewPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(OpenCatCameraNamePayload.ID, OpenCatCameraNamePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(OpenCatCameraListPayload.ID, OpenCatCameraListPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(CatCameraActionResultPayload.ID, CatCameraActionResultPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(CatCameraViewStatePayload.ID, CatCameraViewStatePayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(CreateCatCameraChannelPayload.ID,
                (payload, context) -> context.server().execute(() -> create(context.player(), payload)));
        ServerPlayNetworking.registerGlobalReceiver(EnterCatCameraChannelPayload.ID,
                (payload, context) -> context.server().execute(() -> enter(context.player(), payload)));
        ServerPlayNetworking.registerGlobalReceiver(ExitCatCameraViewPayload.ID,
                (payload, context) -> context.server().execute(() -> CatCameraViewManager.exit(context.player(), true)));
    }

    private static void create(ServerPlayerEntity player, CreateCatCameraChannelPayload payload) {
        if (!hasTerminal(player)) {
            result(player, false, false, "message.neko-technology.cat_camera.no_terminal");
            return;
        }
        Entity entity = player.getServerWorld().getEntity(payload.catUuid());
        if (!(entity instanceof CatEntity cat) || !cat.isAlive() || player.squaredDistanceTo(cat) > 36.0D) {
            result(player, false, false, "message.neko-technology.cat_camera.cat_too_far");
            return;
        }
        if (!cat.isTamed() || !cat.isOwner(player)) {
            result(player, false, false, "message.neko-technology.cat_camera.not_owner");
            return;
        }
        CatCameraChannelAccess access = (CatCameraChannelAccess) cat;
        if (access.neko_technology$isCatCameraChannelActive()) {
            result(player, false, true, "message.neko-technology.cat_camera.already_bound");
            return;
        }

        String name = payload.name() == null ? "" : payload.name().strip();
        int codePoints = name.codePointCount(0, name.length());
        if (codePoints < 1 || codePoints > 16 || name.codePoints().anyMatch(Character::isISOControl)) {
            result(player, false, false, "message.neko-technology.cat_camera.invalid_name");
            return;
        }
        CatCameraChannelWorldState state = CatCameraChannelWorldState.get(player.getServer());
        CatCameraChannelData conflict = state.findActiveByNormalizedName(CatCameraChannelData.normalizeName(name));
        if (conflict != null) {
            result(player, false, false, "message.neko-technology.cat_camera.duplicate_name");
            return;
        }

        CatCameraChannelService.create(cat, player.getUuid(), name);
        result(player, true, true, "message.neko-technology.cat_camera.created");
    }

    private static void enter(ServerPlayerEntity player, EnterCatCameraChannelPayload payload) {
        if (!hasTerminal(player)) {
            result(player, false, false, "message.neko-technology.cat_camera.no_terminal");
            return;
        }
        CatCameraViewManager.enter(player, payload.catUuid());
    }

    private static boolean hasTerminal(ServerPlayerEntity player) {
        return player.getMainHandStack().isOf(ModItems.NEKO_CAT_CAMERA_TERMINAL)
                || player.getOffHandStack().isOf(ModItems.NEKO_CAT_CAMERA_TERMINAL);
    }

    private static void result(ServerPlayerEntity player, boolean success, boolean close, String key) {
        ServerPlayNetworking.send(player, new CatCameraActionResultPayload(success, close, key));
    }
}

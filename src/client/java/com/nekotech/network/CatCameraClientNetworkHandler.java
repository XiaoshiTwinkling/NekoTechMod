package com.nekotech.network;

import com.nekotech.catcamera.CatCameraClientState;
import com.nekotech.network.payload.c2s.ExitCatCameraViewPayload;
import com.nekotech.network.payload.s2c.CatCameraActionResultPayload;
import com.nekotech.network.payload.s2c.CatCameraViewStatePayload;
import com.nekotech.network.payload.s2c.OpenCatCameraListPayload;
import com.nekotech.network.payload.s2c.OpenCatCameraNamePayload;
import com.nekotech.screens.CatCameraChannelListScreen;
import com.nekotech.screens.CatCameraNameScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class CatCameraClientNetworkHandler {
    private static KeyBinding exitKey;
    private CatCameraClientNetworkHandler() {}

    public static void initialize() {
        exitKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.neko-technology.cat_camera.exit", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V,
                "category.neko-technology.cat_camera"));

        ClientPlayNetworking.registerGlobalReceiver(OpenCatCameraNamePayload.ID, (payload, context) ->
                context.client().execute(() -> context.client().setScreen(new CatCameraNameScreen(payload.catUuid()))));
        ClientPlayNetworking.registerGlobalReceiver(OpenCatCameraListPayload.ID, (payload, context) ->
                context.client().execute(() -> context.client().setScreen(new CatCameraChannelListScreen(payload.channels()))));
        ClientPlayNetworking.registerGlobalReceiver(CatCameraActionResultPayload.ID, CatCameraClientNetworkHandler::handleResult);
        ClientPlayNetworking.registerGlobalReceiver(CatCameraViewStatePayload.ID, (payload, context) -> context.client().execute(() -> {
            CatCameraClientState.set(payload.active(), payload.catUuid());
            if (payload.active()) context.client().setScreen(null);
        }));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> CatCameraClientState.set(false, null));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!CatCameraClientState.isActive()) return;
            while (exitKey.wasPressed()) ClientPlayNetworking.send(new ExitCatCameraViewPayload());
            lockInputs(client);
        });

        HudRenderCallback.EVENT.register((context, tickCounter) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (CatCameraClientState.isActive() && client.textRenderer != null) {
                context.drawCenteredTextWithShadow(client.textRenderer,
                        Text.translatable("hud.neko-technology.cat_camera.exit"),
                        client.getWindow().getScaledWidth() / 2,
                        client.getWindow().getScaledHeight() - 35,
                        0xFFFFFF);
            }
        });
    }

    private static void handleResult(CatCameraActionResultPayload payload, ClientPlayNetworking.Context context) {
        context.client().execute(() -> {
            if (context.client().currentScreen instanceof CatCameraNameScreen screen) {
                screen.applyResult(payload.success(), payload.closeScreen(), payload.messageKey());
            } else if (context.client().currentScreen instanceof CatCameraChannelListScreen screen) {
                screen.setError(payload.messageKey());
            } else if (context.client().player != null) {
                context.client().player.sendMessage(Text.translatable(payload.messageKey()), true);
            }
        });
    }

    private static void lockInputs(MinecraftClient client) {
        var options = client.options;
        options.forwardKey.setPressed(false); options.backKey.setPressed(false);
        options.leftKey.setPressed(false); options.rightKey.setPressed(false);
        options.jumpKey.setPressed(false); options.sneakKey.setPressed(false); options.sprintKey.setPressed(false);
        options.attackKey.setPressed(false); options.useKey.setPressed(false); options.pickItemKey.setPressed(false);
        options.inventoryKey.setPressed(false); options.dropKey.setPressed(false); options.swapHandsKey.setPressed(false);
        for (KeyBinding key : options.hotbarKeys) key.setPressed(false);
    }
}

package com.nekotech.client.hud;

import com.nekotech.item.api.googles.GoogleAbstractHUD;
import com.nekotech.item.api.googles.IHaveGoogleHUD;
import com.nekotech.item.custom.GogglesItem;
import com.nekotech.network.ClientHudNetworkHandler;
import com.nekotech.network.HudDataCache;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

/**
 * 处理护目镜HUD的渲染喵~~~~~~~~~~~~~~~~~~~~~
 * 终于到这一步了QWQWQWQQWQWQWQWQWQWQWQ
 */
public class GogglesHudRenderer implements HudRenderCallback {
    private GoogleAbstractHUDClient currentHUD = null;
    private long lastRequestTime = 0;
    private BlockPos lastRequestedPos = null;

    @Override
    public void onHudRender(DrawContext drawContext, RenderTickCounter renderTickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            currentHUD = null;
            return;
        }

        //检查是否戴着护目镜喵
        if (!GogglesItem.isWearingGoggles(client.player)) {
            currentHUD = null;
            return;
        }

        //获取准星目标喵
        HitResult hit = client.crosshairTarget;
        if (!(hit instanceof BlockHitResult blockHit)) {
            currentHUD = null;
            return;
        }

        var pos = blockHit.getBlockPos();

//检查方块是否支持HUD喵
//        if (!(blockEntity instanceof IHaveGoogleHUD)) {
//            currentHUD = null;
//            HudDataCache.removeHudData(pos);
//            return;
//        }

        long now = System.currentTimeMillis();
        if (!pos.equals(lastRequestedPos) || now - lastRequestTime > 100) { //每秒10次
            ClientHudNetworkHandler.requestHudData(pos);
            lastRequestedPos = pos;
            lastRequestTime = now;
        }

        java.util.List<GoogleAbstractHUD> hudList = HudDataCache.getHudDataList(pos);

        if (hudList != null && !hudList.isEmpty()) {
            int screenWidth = client.getWindow().getScaledWidth();
            int screenHeight = client.getWindow().getScaledHeight();
            int margin = 10;
            for (GoogleAbstractHUD hudData : hudList) {
                GoogleAbstractHUDClient hudClient = null;
                if (currentHUD != null && currentHUD.isSame(hudData)) {
                    currentHUD.update(hudData);
                    hudClient = currentHUD;
                } else {
                    hudClient = HudFactory.createHUD(hudData);
                }

                if (hudClient != null) {
                    hudClient.calculatePosition(screenWidth, screenHeight, margin);

                    float tickDelta = renderTickCounter.getTickDelta(true);
                    hudClient.render(drawContext, tickDelta);

                    currentHUD = hudClient;
                }
            }
        } else {
            currentHUD = null;
        }
    }
}
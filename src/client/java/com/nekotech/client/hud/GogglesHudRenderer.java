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

/**
 * 处理护目镜HUD的渲染喵~~~~~~~~~~~~~~~~~~~~~
 * 终于到这一步了QWQWQWQQWQWQWQWQWQWQWQ
 */
public class GogglesHudRenderer implements HudRenderCallback {
    private GoogleAbstractHUDClient currentHUD = null;
    private long lastRequestTime = 0;

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
        var blockEntity = client.world.getBlockEntity(pos);
        if (!(blockEntity instanceof IHaveGoogleHUD)) {
            currentHUD = null;
            HudDataCache.removeHudData(pos);
            return;
        }

        // 1. 获取缓存中的HUD数据
        GoogleAbstractHUD hubData = HudDataCache.getHudData(pos);

        // 2. 如果没有缓存或已过期，请求新数据
        if (hubData == null || !HudDataCache.isDataValid(pos)) {
            long now = System.currentTimeMillis();
            if (now - lastRequestTime > 1000) { // 每秒最多请求一次
                ClientHudNetworkHandler.requestHudData(pos);
                lastRequestTime = now;
            }
            currentHUD = null;
            return;
        }

        // 3. 通过工厂创建客户端HUD喵
        if (hubData != null) {
            currentHUD = HudFactory.createHUD(hubData);
        } else {
            currentHUD = null;
        }

        // 4. 设置HUD位置喵（屏幕右上角）
        if (currentHUD != null) {
            int screenWidth = client.getWindow().getScaledWidth();
            int x = screenWidth - currentHUD.width - 10;
            int y = 10;
            currentHUD.setPosition(x, y);

            float tickDelta = renderTickCounter.getTickDelta(true);
            // 渲染HUD
            currentHUD.render(drawContext, tickDelta);
        }
    }
}

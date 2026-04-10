package com.nekotech;

import com.nekotech.block.entity.ModBlockEntities;
import com.nekotech.client.hud.GogglesHudRenderer;
import com.nekotech.network.ClientHudNetworkHandler;
import com.nekotech.renderer.AlloyPotBlockEntityRenderer;
import com.nekotech.renderer.BellowsBlockEntityRenderer;
import com.nekotech.renderer.CatTailFeatureRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.client.render.entity.PlayerEntityRenderer;

public class NekoTechnologyClient implements ClientModInitializer {

    private static final GogglesHudRenderer HUD_RENDERER = new GogglesHudRenderer();

	@Override
	public void onInitializeClient() {
		NekoTechnology.LOGGER.info("NekoTechnologyClient initialized");

        BlockEntityRendererFactories.register(
                ModBlockEntities.bellows,
                BellowsBlockEntityRenderer::new
        );

        BlockEntityRendererFactories.register(
                ModBlockEntities.alloy_pot,
                AlloyPotBlockEntityRenderer::new
        );

        ClientHudNetworkHandler.initialize();
        // 注册HUD渲染回调
        HudRenderCallback.EVENT.register(HUD_RENDERER);


        LivingEntityFeatureRendererRegistrationCallback.EVENT.register(
                (entityType, renderer, helper, context) -> {
                    if (renderer instanceof PlayerEntityRenderer playerRenderer) {
                        helper.register(new CatTailFeatureRenderer(playerRenderer));
                    }
                }
        );
	}

    public static GogglesHudRenderer getHudRenderer() {
        return HUD_RENDERER;
    }
}
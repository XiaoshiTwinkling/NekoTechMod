package com.nekotech;

import com.nekotech.Screen.NekoTagScreen;
import com.nekotech.block.entity.ModBlockEntities;
import com.nekotech.renderer.GogglesHudRenderer;
import com.nekotech.network.ClientHudNetworkHandler;
import com.nekotech.renderer.AlloyPotBlockEntityRenderer;
import com.nekotech.renderer.BellowsBlockEntityRenderer;
import com.nekotech.renderer.CatTailFeatureRenderer;
import com.nekotech.renderer.ClientLaserTargetCache;
import com.nekotech.renderer.components.ComponentAttachmentRenderer;
import com.nekotech.screen.ModScreenHandlers;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
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

        registerComponentAttachmentRenderer();

        ClientHudNetworkHandler.initialize();
        // 注册HUD渲染回调
        HudRenderCallback.EVENT.register(HUD_RENDERER);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ClientLaserTargetCache.tick();
        });

        WorldRenderEvents.AFTER_TRANSLUCENT.register(ClientLaserTargetCache::render);


        LivingEntityFeatureRendererRegistrationCallback.EVENT.register(
                (entityType, renderer, helper, context) -> {
                    if (renderer instanceof PlayerEntityRenderer playerRenderer) {
                        helper.register(new CatTailFeatureRenderer(playerRenderer));
                    }
                }
        );
        HandledScreens.register(ModScreenHandlers.NEKO_TAG, NekoTagScreen::new);
	}

    public static GogglesHudRenderer getHudRenderer() {
        return HUD_RENDERER;
    }

    private void registerComponentAttachmentRenderer(){
        BlockEntityRendererRegistry.register(ModBlockEntities.flux_storage, new BlockEntityRendererFactory<BlockEntity>() {
            @Override
            public BlockEntityRenderer<BlockEntity> create(Context ctx) {
                return new ComponentAttachmentRenderer();
            }
        });
    }
}
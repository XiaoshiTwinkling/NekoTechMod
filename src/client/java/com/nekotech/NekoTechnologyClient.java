package com.nekotech;

import com.nekotech.block.entity.ModBlockEntities;
import com.nekotech.item.ModItems;
import com.nekotech.item.block.ModBlocks;
import com.nekotech.item.custom.NekoTag.NekoTagData;
import com.nekotech.renderer.*;
import com.nekotech.network.ClientHudNetworkHandler;
import com.nekotech.renderer.components.ComponentAttachmentRenderer;
import com.nekotech.screen.ModScreenHandlers;
import com.nekotech.screens.NekoTagScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.util.Identifier;


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

        registerRenderLayerMap();

        LivingEntityFeatureRendererRegistrationCallback.EVENT.register(
                (entityType, renderer, helper, context) -> {
                    if (renderer instanceof PlayerEntityRenderer playerRenderer) {
                        helper.register(new CatTailFeatureRenderer(playerRenderer));
                    }
                }
        );
        HandledScreens.register(ModScreenHandlers.NEKO_TAG, NekoTagScreen::new);
        BlockEntityRendererRegistry.register(ModBlockEntities.work_bench, WorkBenchBlockEntityRenderer::new);

        ModelPredicateProviderRegistry.register(
                ModItems.neko_tag,
                Identifier.of("neko-technology", "color"),
                (stack, world, entity, seed) -> NekoTagData.readColor(stack).getId()/15.0F

        );
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
        BlockEntityRendererRegistry.register(ModBlockEntities.basic_storage_enclosure, new BlockEntityRendererFactory<BlockEntity>() {
            @Override
            public BlockEntityRenderer<BlockEntity> create(Context ctx) {
                return new ComponentAttachmentRenderer();
            }
        });
        BlockEntityRendererRegistry.register(ModBlockEntities.heater, new BlockEntityRendererFactory<BlockEntity>() {
            @Override
            public BlockEntityRenderer<BlockEntity> create(Context ctx) {
                return new ComponentAttachmentRenderer();
            }
        });

        BlockEntityRendererRegistry.register(ModBlockEntities.coil_block, new BlockEntityRendererFactory<BlockEntity>() {
            @Override
            public BlockEntityRenderer<BlockEntity> create(Context ctx) {
                return new ComponentAttachmentRenderer();
            }
        });
    }

    private void registerRenderLayerMap(){
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.work_bench, RenderLayer.getTranslucent());
    }
}
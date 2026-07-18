package com.nekotech;

import com.nekotech.block.ModBlocks;
import com.nekotech.block.entity.ModBlockEntities;
import com.nekotech.item.ModItems;
import com.nekotech.item.custom.NekoTag.NekoTagData;
import com.nekotech.network.ClientHudNetworkHandler;
import com.nekotech.renderer.*;
import com.nekotech.renderer.blockentities.*;
import com.nekotech.renderer.components.ComponentAttachmentRenderer;
import com.nekotech.renderer.components.WirePoleRenderer;
import com.nekotech.screen.ModScreenHandlers;
import com.nekotech.screens.NekoTagScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
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
import net.minecraft.client.render.entity.CatEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.util.Identifier;

public class NekoTechnologyClient implements ClientModInitializer {

    private static final GogglesHudRenderer HUD_RENDERER = new GogglesHudRenderer();

    @Override
    public void onInitializeClient() {
        NekoTechnology.LOGGER.info("NekoTechnologyClient initialized");

        registerBlockEntityRenderers();
        registerComponentAttachmentRenderers();
        registerRenderLayerMap();
        registerModelPredicates();
        registerEventCallbacks();
        registerScreens();
        registerNetworkHandlers();
    }

    private void registerBlockEntityRenderers() {
        BlockEntityRendererFactories.register(
                ModBlockEntities.BELLOWS,
                BellowsBlockEntityRenderer::new
        );

        BlockEntityRendererFactories.register(
                ModBlockEntities.ALLOY_POT,
                AlloyPotBlockEntityRenderer::new
        );

        BlockEntityRendererFactories.register(
                ModBlockEntities.COIL_BLOCK,
                CoilBlockEntityRenderer::new
        );

        BlockEntityRendererFactories.register(
                ModBlockEntities.ELEVATOR_CORE_BLOCK_ENTITY,
                ElevatorCoreBlockEntityRenderer::new
        );

        BlockEntityRendererFactories.register(
                ModBlockEntities.CAT_GENERATOR,
                CatGeneratorTrackRenderer::new
        );

        BlockEntityRendererFactories.register(
                ModBlockEntities.WORK_BENCH,
                WorkBenchBlockEntityRenderer::new
        );

        BlockEntityRendererFactories.register(
                ModBlockEntities.ADVANCED_WORK_BENCH,
                WorkBenchBlockEntityRenderer::new
        );

        BlockEntityRendererFactories.register(
                ModBlockEntities.CHARGE_STATION,
                ChargeStationBlockEntityRenderer::new
        );
    }


    private void registerComponentAttachmentRenderers() {
        WirePoleRenderer.initialize();

        BlockEntityRendererFactories.register(
                ModBlockEntities.FLUX_STORAGE,
                new BlockEntityRendererFactory<BlockEntity>() {
                    @Override
                    public BlockEntityRenderer<BlockEntity> create(Context ctx) {
                        return new ComponentAttachmentRenderer();
                    }
                }
        );

        BlockEntityRendererFactories.register(
                ModBlockEntities.HEATER,
                new BlockEntityRendererFactory<BlockEntity>() {
                    @Override
                    public BlockEntityRenderer<BlockEntity> create(Context ctx) {
                        return new ComponentAttachmentRenderer();
                    }
                }
        );
    }

    private void registerRenderLayerMap() {
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.WORK_BENCH, RenderLayer.getTranslucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.ADVANCED_WORK_BENCH, RenderLayer.getTranslucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.ELEVATOR_CORE_BLOCK, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.ELEVATOR_PART_BLOCK, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.CATNIP_CROP, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.PETGRASS_CROP, RenderLayer.getCutout());
    }


    private void registerModelPredicates() {
        ModelPredicateProviderRegistry.register(
                ModItems.NEKO_TAG,
                Identifier.of("neko-technology", "color"),
                (stack, world, entity, seed) -> NekoTagData.readColor(stack).getId() / 15.0F
        );
    }


    private void registerEventCallbacks() {
        // HUD 渲染
        HudRenderCallback.EVENT.register(HUD_RENDERER);

        // 客户端刻结束事件
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ClientLaserTargetCache.tick();
        });

        // 透明层后渲染激光目标
        WorldRenderEvents.AFTER_TRANSLUCENT.register(ClientLaserTargetCache::render);

        // 实体特征渲染器注册
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register(
                (entityType, renderer, helper, context) -> {
                    if (renderer instanceof PlayerEntityRenderer playerRenderer) {
                        helper.register(new CatTailFeatureRenderer(playerRenderer));
                    }

                    if (renderer instanceof CatEntityRenderer catRenderer) {
                        helper.register(new NekoMarkFeatureRenderer(catRenderer));
                    }
                }
        );
    }


    private void registerScreens() {
        HandledScreens.register(ModScreenHandlers.NEKO_TAG, NekoTagScreen::new);
    }


    private void registerNetworkHandlers() {
        ClientHudNetworkHandler.initialize();
    }
}
package com.nekotech;

import com.nekotech.block.entity.ModBlockEntities;
import com.nekotech.renderer.AlloyPotBlockEntityRenderer;
import com.nekotech.renderer.BellowsBlockEntityRenderer;
import com.nekotech.renderer.CatTailFeatureRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.client.render.entity.PlayerEntityRenderer;

public class NekoTechnologyClient implements ClientModInitializer {
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

        LivingEntityFeatureRendererRegistrationCallback.EVENT.register(
                (entityType, renderer, helper, context) -> {
                    if (renderer instanceof PlayerEntityRenderer playerRenderer) {
                        helper.register(new CatTailFeatureRenderer(playerRenderer));
                    }
                }
        );
	}
}